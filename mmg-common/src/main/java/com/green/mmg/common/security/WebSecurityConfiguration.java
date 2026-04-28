package com.green.mmg.common.security;

import com.green.mmg.common.jwt.TokenAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

// Spring Security 전체 설정을 담당하는 클래스
// 차단할 것만 명시하고, 나머지는 전부 허용하는 블랙리스트 방식
@Configuration
@RequiredArgsConstructor
public class WebSecurityConfiguration {

    // 모든 요청마다 쿠키에서 JWT 를 꺼내 인증 처리하는 필터
    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // JWT 사용 → 서버에서 세션을 만들지 않음
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Spring Security 기본 로그인 화면 비활성화
                .httpBasic(hb -> hb.disable())
                // form 로그인 비활성화 (REST API 방식 사용)
                .formLogin(fl -> fl.disable())
                // CSRF 비활성화 (REST API + JWT 방식에서는 불필요)
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(req -> req

                        // ── OWNER 전용 (사장만 접근 가능) ──
                        .requestMatchers("/api/owner/**").hasRole("OWNER")

                        // ── CUSTOMER 전용 (고객만 접근 가능) ──
                        .requestMatchers("/api/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/order/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/favorite/**").hasRole("CUSTOMER")

                        // ── 로그인 필수 (역할 무관) ──
                        .requestMatchers(HttpMethod.POST, "/api/review/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/user/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/user/**").authenticated()

                        // ── 나머지는 전부 허용 ──
                        .anyRequest().permitAll()
                )
                // Spring Security 기본 로그인 필터 앞에 JWT 필터를 끼워 넣음
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 프론트엔드 주소 허용
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 자격 증명(쿠키, 인증 헤더) 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 비밀번호 암호화에 사용할 인코더 빈 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
