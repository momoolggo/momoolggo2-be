package com.green.mmg.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Phase 4-B: Gateway 단일 진입점에서 CORS 처리.
 *
 * <p>각 서비스(BaseSecurityConfig)에도 CORS가 등록되어 있지만 Gateway 경유 시 Gateway가 우선 처리.
 * 외부 요청은 Gateway만 받으므로 서비스 자체 CORS는 사실상 미사용 (이중 안전).
 * 서비스 CORS 제거는 Phase 5 검토.</p>
 */
@Configuration
public class GatewayCorsConfig {

    @Value("#{'${cors.allowed-origins:http://localhost:5173}'.split(',')}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(Arrays.asList("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(source);
    }
}
