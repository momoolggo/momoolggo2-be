package com.green.mmg.common.security;

import com.green.mmg.common.constants.ConstJwt;
import com.green.mmg.common.jwt.TokenAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

// MSA 공용 시큐리티 베이스.
// - spring-security가 classpath에 있는 서비스에서만 활성화 (@ConditionalOnClass)
// - ConstJwt도 같이 활성 (시큐리티 안 쓰는 서비스는 JWT 설정 yml 키 불필요)
// - 자식 서비스가 자기 SecurityFilterChain 빈을 등록하면 default는 자동 비활성 (@ConditionalOnMissingBean)
@Configuration
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(ConstJwt.class)
@RequiredArgsConstructor
public class BaseSecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final AuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final AccessDeniedHandler jsonAccessDeniedHandler;

    // 콤마 구분 다중 origin → List<String>
    @Value("#{'${cors.allowed-origins:http://localhost:5173}'.split(',')}")
    private List<String> allowedOrigins;

    /**
     * 자식 서비스가 호출하는 공통 시큐리티 적용.
     * authorize 룰만 자식이 정의 후 .build() 호출.
     */
    public HttpSecurity applyCommon(HttpSecurity http) throws Exception {
        return http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c.disable())
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return applyCommon(http)
                .authorizeHttpRequests(req -> req.anyRequest().permitAll())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(Arrays.asList("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
