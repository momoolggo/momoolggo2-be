package com.green.mmg.main.config;

import com.green.mmg.common.security.BaseSecurityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class MainSecurityConfig {

    private final BaseSecurityConfig base;

    @Bean
    public SecurityFilterChain mainFilterChain(HttpSecurity http) throws Exception {
        return base.applyCommon(http)
                .authorizeHttpRequests(req -> req
                        // 정적 리소스 (이미지 조회 — 비로그인 가능)
                        .requestMatchers("/uploads/**").permitAll()

                        // 가게 조회 (목록/상세/메뉴/검색은 누구나)
                        .requestMatchers(HttpMethod.GET, "/api/store/**").permitAll()

                        // OWNER 전용 (사장 관리)
                        .requestMatchers("/api/owner/**").hasRole("OWNER")

                        // CUSTOMER 전용 (Phase 2-C에서 cart/order 코드 추가 시 활성화 예정)
                        .requestMatchers("/api/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/order/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/payment/**").hasRole("CUSTOMER")

                        // 리뷰 작성/수정/삭제는 인증 (Phase 2-E에서 활성화 예정)
                        .requestMatchers(HttpMethod.POST, "/api/user/review/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/user/review/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/user/review/**").authenticated()

                        // 헬스/임시
                        .requestMatchers("/actuator/health").permitAll()
                        // 서버간 통신 internal API
                        .requestMatchers("/internal/**").permitAll()


                        .anyRequest().authenticated()
                )
                .build();
    }
}
