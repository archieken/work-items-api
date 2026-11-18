package com.elemental.tech.workitems.requestlog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_log")
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 2048)
    private String requestUri;

    @Column(nullable = false)
    private Instant requestTimestamp;

    @Column(nullable = false)
    private int responseCode;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 2)
    private String countryCode;

    @Column(length = 255)
    private String ipProvider;

    @Column(nullable = false)
    private long timeLapsedMs;

    protected RequestLog() {
    }

    public RequestLog(String requestUri, Instant requestTimestamp, int responseCode, String ipAddress,
                      String countryCode, String ipProvider, long timeLapsedMs) {
        this.requestUri = requestUri;
        this.requestTimestamp = requestTimestamp;
        this.responseCode = responseCode;
        this.ipAddress = ipAddress;
        this.countryCode = countryCode;
        this.ipProvider = ipProvider;
        this.timeLapsedMs = timeLapsedMs;
    }

    public UUID getId() {
        return id;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public Instant getRequestTimestamp() {
        return requestTimestamp;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getIpProvider() {
        return ipProvider;
    }

    public long getTimeLapsedMs() {
        return timeLapsedMs;
    }
}
