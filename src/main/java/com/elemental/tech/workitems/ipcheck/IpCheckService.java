package com.elemental.tech.workitems.ipcheck;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IpCheckService {

    private final IpApiClient client;
    private final IpCheckProperties properties;

    public IpCheckService(IpApiClient client, IpCheckProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public IpCheckResult check(String ip) {
        IpLookup lookup = client.lookup(ip);

        if (lookup == null || !lookup.isSuccess()) {
            return IpCheckResult.allowed(null, null);
        }

        String countryCode = lookup.countryCode();
        String isp = lookup.isp();

        if (countryCode != null && properties.blockedCountries().contains(countryCode.toUpperCase(Locale.ROOT))) {
            return IpCheckResult.blocked(
                    "Requests from " + lookup.country() + " (" + countryCode + ") are not allowed", countryCode, isp);
        }

        String provider = matchBlockedProvider(lookup);
        if (provider != null) {
            return IpCheckResult.blocked(
                    "Requests from " + provider + " data centres are not allowed", countryCode, isp);
        }

        return IpCheckResult.allowed(countryCode, isp);
    }

    private String matchBlockedProvider(IpLookup lookup) {
        String haystack = Stream.of(lookup.isp(), lookup.org(), lookup.asName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));

        for (Map.Entry<String, List<String>> provider : properties.blockedProviders().entrySet()) {
            for (String keyword : provider.getValue()) {
                var wholeWord = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b", Pattern.CASE_INSENSITIVE);
                if (wholeWord.matcher(haystack).find()) {
                    return provider.getKey();
                }
            }
        }
        return null;
    }
}
