package com.elemental.tech.workitems.ipcheck;

import com.elemental.tech.workitems.requestlog.RequestLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(IpCheckProperties.class)
class IpCheckConfig {

    @Bean
    IpApiClient ipApiClient(RestClient.Builder builder, IpCheckProperties properties) {
        return new IpApiClient(builder, properties);
    }

    @Bean
    IpCheckService ipCheckService(IpApiClient client, IpCheckProperties properties) {
        return new IpCheckService(client, properties);
    }

    @Bean
    FilterRegistrationBean<IpCheckFilter> ipCheckFilter(IpCheckService service, RequestLogRepository repository,
                                                        ObjectMapper objectMapper) {
        var registration = new FilterRegistrationBean<>(new IpCheckFilter(service, repository, objectMapper));
        registration.addUrlPatterns("/api/work-items", "/api/work-items/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
