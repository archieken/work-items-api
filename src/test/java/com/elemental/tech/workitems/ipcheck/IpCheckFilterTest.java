package com.elemental.tech.workitems.ipcheck;

import com.elemental.tech.workitems.requestlog.RequestLog;
import com.elemental.tech.workitems.requestlog.RequestLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IpCheckFilterTest {

    IpCheckService ipCheckService = mock(IpCheckService.class);
    RequestLogRepository repository = mock(RequestLogRepository.class);
    IpCheckFilter filter = new IpCheckFilter(ipCheckService, repository, new ObjectMapper());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/work-items/1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    @Test
    void allowed() throws Exception {
        request.setRemoteAddr("81.2.69.160");
        when(ipCheckService.check("81.2.69.160")).thenReturn(IpCheckResult.allowed("GB", "BT"));

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());

        RequestLog log = savedLog();
        assertEquals("/api/work-items/1", log.getRequestUri());
        assertEquals(200, log.getResponseCode());
        assertEquals("81.2.69.160", log.getIpAddress());
        assertEquals("GB", log.getCountryCode());
        assertEquals("BT", log.getIpProvider());
    }

    @Test
    void blocked() throws Exception {
        when(ipCheckService.check(any()))
                .thenReturn(IpCheckResult.blocked("Requests from China (CN) are not allowed", "CN", "China Telecom"));

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest());
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Requests from China (CN) are not allowed"));
        assertEquals(403, savedLog().getResponseCode());
    }

    @Test
    void ipApiDown() throws Exception {
        when(ipCheckService.check(any())).thenThrow(new ResourceAccessException("timed out"));

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest());
        assertEquals(503, response.getStatus());
        assertEquals(503, savedLog().getResponseCode());
    }

    @Test
    void controllerThrows() throws Exception {
        when(ipCheckService.check(any())).thenReturn(IpCheckResult.allowed("GB", "BT"));
        var failingChain = mock(FilterChain.class);
        doThrow(new IllegalStateException()).when(failingChain).doFilter(any(), any());

        assertThrows(IllegalStateException.class, () -> filter.doFilter(request, response, failingChain));
        assertEquals(500, savedLog().getResponseCode());
    }

    @Test
    void dbErrorDoesntFailRequest() throws Exception {
        when(ipCheckService.check(any())).thenReturn(IpCheckResult.allowed("GB", "BT"));
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    private RequestLog savedLog() {
        var captor = ArgumentCaptor.forClass(RequestLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
