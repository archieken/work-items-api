package com.elemental.tech.workitems.ipcheck;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public class IpApiClient {

    private static final String FIELDS = "status,message,country,countryCode,isp,org,as";

    private final RestClient restClient;

    public IpApiClient(RestClient.Builder builder, IpCheckProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());

        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public IpLookup lookup(String ip) {
        return restClient.get()
                .uri("/json/{ip}?fields={fields}", ip, FIELDS)
                .retrieve()
                .body(IpLookup.class);
    }
}
