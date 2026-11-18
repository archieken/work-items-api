package com.elemental.tech.workitems.ipcheck;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IpLookup(
        String status,
        String message,
        String country,
        String countryCode,
        String isp,
        String org,
        @JsonProperty("as") String asName
) {

    public boolean isSuccess() {
        return "success".equals(status);
    }
}
