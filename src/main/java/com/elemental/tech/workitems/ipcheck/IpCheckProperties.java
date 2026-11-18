package com.elemental.tech.workitems.ipcheck;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "ip-check")
public record IpCheckProperties(
        String baseUrl,
        Duration timeout,
        Set<String> blockedCountries,
        Map<String, List<String>> blockedProviders
) {
}
