package com.green.mmg.common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//filter는 요청, 응답이 무조건 filter를 거치게 된다. 거칠 때 하고 싶은 작업을 진행하면 된다.
// 여기서는 쿠키안에 AT가 저장되어 있는지 확인하고 저장되어 있으면 시큐리티 인증처리를 한다.
@Slf4j
@Component
@ConditionalOnClass(SecurityFilterChain.class)
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenManager jwtTokenManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("req-uri: {}", request.getRequestURI());

        try {
            Authentication authentication = jwtTokenManager.getAuthentication(request);
            log.info("authentication: {}", authentication);
            if(authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 잘못된 JWT 토큰이 쿠키에 남아있어도 필터가 멈추지 않고 계속 진행
            log.warn("JWT 인증 실패 (무시): {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
