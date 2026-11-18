package com.elemental.tech.workitems.ipcheck;

public record IpCheckResult(boolean blocked, String reason, String countryCode, String isp) {

    static IpCheckResult allowed(String countryCode, String isp) {
        return new IpCheckResult(false, null, countryCode, isp);
    }

    static IpCheckResult blocked(String reason, String countryCode, String isp) {
        return new IpCheckResult(true, reason, countryCode, isp);
    }
}
