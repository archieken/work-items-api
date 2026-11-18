package com.elemental.tech.workitems.ipcheck;

import com.elemental.tech.workitems.requestlog.RequestLog;
import com.elemental.tech.workitems.requestlog.RequestLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

public class IpCheckFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IpCheckFilter.class);

    private final IpCheckService ipCheckService;
    private final RequestLogRepository requestLogRepository;
    private final ObjectMapper objectMapper;

    public IpCheckFilter(IpCheckService ipCheckService, RequestLogRepository requestLogRepository,
                         ObjectMapper objectMapper) {
        this.ipCheckService = ipCheckService;
        this.requestLogRepository = requestLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Instant receivedAt = Instant.now();
        long start = System.nanoTime();
        String ip = request.getRemoteAddr();
        IpCheckResult result = null;
        boolean completed = false;

        try {
            try {
                result = ipCheckService.check(ip);
            } catch (RestClientException e) {
                log.warn("IP lookup failed for {}", ip, e);
                writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify client IP address");
                completed = true;
                return;
            }

            if (result.blocked()) {
                writeError(response, HttpStatus.FORBIDDEN, result.reason());
            } else {
                chain.doFilter(request, response);
            }
            completed = true;
        } finally {
            int status = completed ? response.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR.value();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            saveLog(new RequestLog(
                    request.getRequestURI(),
                    receivedAt,
                    status,
                    ip,
                    result == null ? null : result.countryCode(),
                    result == null ? null : result.isp(),
                    elapsedMs));
        }
    }

    private void saveLog(RequestLog entry) {
        try {
            requestLogRepository.save(entry);
        } catch (RuntimeException e) {
            log.error("Failed to save request log for {}", entry.getRequestUri(), e);
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", detail);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
