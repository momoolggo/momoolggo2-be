package com.green.mmg.gateway.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
public class GatewayCorsConfig {

    private static final Set<String> CORS_HEADERS = Set.of(
            "access-control-allow-origin",
            "access-control-allow-credentials",
            "access-control-allow-methods",
            "access-control-allow-headers",
            "access-control-max-age"
    );

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String rawAllowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(parseAllowedOrigins(rawAllowedOrigins));
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(Arrays.asList("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(source);
    }

    /**
     * CORS 헤더 중복 방지 — addHeader 호출을 setHeader로 교체해 마지막 값 하나만 남김.
     * GatewayCorsConfig + BaseSecurityConfig + upstream 서비스가 각각 CORS 헤더를 붙이는 문제 해결.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Filter corsDeduplicatingFilter() {
        return (request, response, chain) -> {
            HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper((HttpServletResponse) response) {
                @Override
                public void addHeader(String name, String value) {
                    if (CORS_HEADERS.contains(name.toLowerCase())) {
                        super.setHeader(name, value);
                    } else {
                        super.addHeader(name, value);
                    }
                }
            };
            chain.doFilter(request, wrapper);
        };
    }

    static List<String> parseAllowedOrigins(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
