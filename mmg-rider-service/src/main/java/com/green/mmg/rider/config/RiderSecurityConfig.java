package com.green.mmg.rider.config;

import com.green.mmg.common.security.BaseSecurityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * rider-service 시큐리티 — auth/main 패턴 일관 (BaseSecurityConfig.applyCommon).
 *
 * <p>{@code /api/rider/**}는 RIDER role 인증 필수.
 * 단, {@code /api/rider/hello}는 Phase 0-B 잔재 — 별도 정리 결정 시 처리.</p>
 *
 * <p>{@code /internal/**}는 Phase 4-B Gateway 차단 정책으로 외부 접근 0.
 * 서비스 자체는 permitAll (auth-service 패턴 일관, R4 진입 시 Internal endpoint 도입 검토).</p>
 */
@Configuration
@RequiredArgsConstructor
public class RiderSecurityConfig {

    private final BaseSecurityConfig base;

    @Bean
    public SecurityFilterChain riderFilterChain(HttpSecurity http) throws Exception {
        return base.applyCommon(http)
                .authorizeHttpRequests(req -> req
                        // 헬스
                        .requestMatchers("/actuator/health").permitAll()

                        // Internal (Phase 4-B Gateway 차단 — R4에서 endpoint 도입 시 검토)
                        .requestMatchers("/internal/**").permitAll()

                        // 라이더 전용 — RIDER role 필요 (가입 직후 PENDING 상태도 role=RIDER)
                        .requestMatchers("/api/rider/**").hasRole("RIDER")

                        .anyRequest().authenticated()
                )
                .build();
    }
}
