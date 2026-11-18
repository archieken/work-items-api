package com.elemental.tech.workitems.ipcheck;

import com.elemental.tech.workitems.requestlog.RequestLog;
import com.elemental.tech.workitems.requestlog.RequestLogRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IpCheckApiTest {

    @RegisterExtension
    static WireMockExtension ipApi = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("ip-check.base-url", ipApi::baseUrl);
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    RequestLogRepository requestLogs;

    @BeforeEach
    void setUp() {
        requestLogs.deleteAll();
    }

    @Test
    void ukRequestIsAllowedAndLogged() {
        ipApiReturns("""
                {"status": "success", "country": "United Kingdom", "countryCode": "GB", "isp": "BT"}
                """);

        var response = rest.getForEntity("/api/work-items", String.class);

        assertEquals(200, response.getStatusCode().value());
        ipApi.verify(getRequestedFor(urlPathMatching("/json/.+")));

        RequestLog log = waitForLog();
        assertEquals("/api/work-items", log.getRequestUri());
        assertEquals(200, log.getResponseCode());
        assertEquals("GB", log.getCountryCode());
        assertEquals("BT", log.getIpProvider());
        assertNotNull(log.getIpAddress());
    }

    @Test
    void spainIsBlocked() {
        ipApiReturns("""
                {"status": "success", "country": "Spain", "countryCode": "ES", "isp": "Telefonica"}
                """);

        var response = rest.getForEntity("/api/work-items", String.class);

        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody().contains("Requests from Spain (ES) are not allowed"));
        assertEquals(403, waitForLog().getResponseCode());
    }

    @Test
    void awsIsBlocked() {
        ipApiReturns("""
                {"status": "success", "country": "Germany", "countryCode": "DE", "isp": "Amazon.com, Inc."}
                """);

        var response = rest.getForEntity("/api/work-items/1", String.class);

        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody().contains("AWS"));
        assertEquals("Amazon.com, Inc.", waitForLog().getIpProvider());
    }

    @Test
    void ipApiError() {
        ipApi.stubFor(get(urlPathMatching("/json/.+")).willReturn(serverError()));

        var response = rest.getForEntity("/api/work-items", String.class);

        assertEquals(503, response.getStatusCode().value());
        assertEquals(503, waitForLog().getResponseCode());
    }

    private void ipApiReturns(String json) {
        ipApi.stubFor(get(urlPathMatching("/json/.+")).willReturn(okJson(json)));
    }

    private RequestLog waitForLog() {
        await().atMost(2, SECONDS).until(() -> requestLogs.count() == 1);
        return requestLogs.findAll().get(0);
    }
}
