package com.green.mmg.auth.security;

import com.green.mmg.common.security.BaseSecurityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class AuthSecurityConfig {

    private final BaseSecurityConfig base;

    @Bean
    public SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
        return base.applyCommon(http)
                .authorizeHttpRequests(req -> req
                        .requestMatchers(
                                "/api/user/login",
                                "/api/user/join",
                                "/api/user/check-id",
                                "/api/user/find-id",
                                "/api/user/reissue",
                                "/api/user/reset-pw",
                                "/api/policy/**",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
