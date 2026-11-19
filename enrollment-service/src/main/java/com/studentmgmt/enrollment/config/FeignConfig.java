package com.studentmgmt.enrollment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmgmt.enrollment.client.RemoteErrorDecoder;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder remoteErrorDecoder(ObjectMapper objectMapper) {
        return new RemoteErrorDecoder(objectMapper);
    }

    /**
     * Forwards the caller's Authorization header to the downstream services.
     * Needs spring.cloud.circuitbreaker.resilience4j.disableThreadPool=true (set in application.yml),
     * otherwise the Feign call may run on another thread where the request context is empty.
     */
    @Bean
    public RequestInterceptor forwardAuthorizationHeader() {
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null) {
                    template.header(HttpHeaders.AUTHORIZATION, auth);
                }
            }
        };
    }
}
