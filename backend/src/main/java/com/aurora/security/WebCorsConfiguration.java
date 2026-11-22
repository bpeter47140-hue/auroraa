package com.aurora.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebCorsConfiguration {
    @Bean
    FilterRegistrationBean<CorsFilter> auroraCorsFilter(
            @Value("${aurora.web.allowed-origin-patterns}") String allowedOriginPatterns) {
        CorsConfiguration cors = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOriginPatterns.split(","))
            .map(String::trim).filter(value -> !value.isEmpty()).toList();
        cors.setAllowedOriginPatterns(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Aurora-Workspace"));
        cors.setExposedHeaders(List.of("Content-Type"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("auroraCorsFilter");
        return registration;
    }
}
