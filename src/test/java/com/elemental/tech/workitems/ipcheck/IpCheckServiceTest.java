package com.elemental.tech.workitems.ipcheck;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IpCheckServiceTest {

    IpApiClient client = mock(IpApiClient.class);
    IpCheckService service;

    {
        var providers = new LinkedHashMap<String, List<String>>();
        providers.put("AWS", List.of("amazon", "aws"));
        providers.put("GCP", List.of("google"));
        providers.put("Azure", List.of("microsoft", "azure"));
        service = new IpCheckService(client,
                new IpCheckProperties("http://localhost", Duration.ofSeconds(1), Set.of("CN", "ES", "US"), providers));
    }

    @Test
    void allowed() {
        lookupReturns("United Kingdom", "GB", "BT");

        var result = service.check("1.2.3.4");

        assertFalse(result.blocked());
        assertEquals("GB", result.countryCode());
        assertEquals("BT", result.isp());
    }

    @Test
    void blockedCountries() {
        lookupReturns("China", "CN", "China Telecom");
        assertEquals("Requests from China (CN) are not allowed", service.check("1.2.3.4").reason());

        lookupReturns("Spain", "ES", "Telefonica");
        assertTrue(service.check("1.2.3.4").blocked());

        lookupReturns("United States", "US", "Comcast");
        assertTrue(service.check("1.2.3.4").blocked());
    }

    @Test
    void aws() {
        when(client.lookup("1.2.3.4")).thenReturn(
                new IpLookup("success", null, "Ireland", "IE", "Amazon.com, Inc.", "AWS EC2 (eu-west-1)", "AS16509"));

        assertEquals("Requests from AWS data centres are not allowed", service.check("1.2.3.4").reason());
    }

    @Test
    void gcpAndAzure() {
        lookupReturns("Belgium", "BE", "Google LLC");
        assertEquals("Requests from GCP data centres are not allowed", service.check("1.2.3.4").reason());

        lookupReturns("Netherlands", "NL", "Microsoft Corporation");
        assertEquals("Requests from Azure data centres are not allowed", service.check("1.2.3.4").reason());
    }

    @Test
    void ispThatJustContainsAwsIsFine() {
        lookupReturns("United Kingdom", "GB", "Hawsons Broadband");

        assertFalse(service.check("1.2.3.4").blocked());
    }

    @Test
    void localhost() {
        when(client.lookup("127.0.0.1")).thenReturn(new IpLookup("fail", "reserved range", null, null, null, null, null));

        assertFalse(service.check("127.0.0.1").blocked());
    }

    private void lookupReturns(String country, String countryCode, String isp) {
        when(client.lookup("1.2.3.4")).thenReturn(new IpLookup("success", null, country, countryCode, isp, null, null));
    }
}
