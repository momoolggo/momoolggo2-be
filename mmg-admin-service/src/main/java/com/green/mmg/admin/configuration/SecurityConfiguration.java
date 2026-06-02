package com.green.mmg.admin.configuration;

import com.green.mmg.common.security.BaseSecurityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final BaseSecurityConfig base;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return base.applyCommon(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/internal/**"          // Gateway가 외부 차단 (InternalBlockController)
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/policy").permitAll()  // 회원가입 약관 조회 공개
                        .requestMatchers(HttpMethod.GET, "/api/admin/cs/faq").permitAll()  // 고객 FAQ 조회 공개
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")  // 외부 무인증 우회 차단
                        .anyRequest().authenticated()
                )
                .build();
    }
}