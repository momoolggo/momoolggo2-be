package com.green.mmg.main.config;

import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.common.security.BaseSecurityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
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

                        // 회원가입 *전* 호출: 주소 검색·역지오코딩 + 지도 SDK key (비로그인 OK)
                        // rate limit 미적용 — NCP 무료 quota 소진 방지는 Phase 6+ tech-debt 등재 (Bucket4j/Gateway 글로벌 필터)
                        .requestMatchers(HttpMethod.GET, "/api/address/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/map/**").permitAll()

                        // 라이더 가입 면허증 사진 업로드 (2026-05-28 트랙) — owner signup-doc 박제 일관, 가입 이전 permitAll
                        .requestMatchers(HttpMethod.POST, "/api/rider-license/upload").permitAll()

                        // OWNER 전용 (사장 관리)
                        .requestMatchers(HttpMethod.POST, "/api/owner/signup-doc/upload").permitAll()
                        .requestMatchers("/api/owner/**").access((authentication, context) -> {
                            var auth = authentication.get();

                            boolean isOwner = auth.getAuthorities().stream()
                                    .anyMatch(a -> "ROLE_OWNER".equals(a.getAuthority()));

                            boolean isActive = auth.getPrincipal() instanceof UserPrincipal principal
                                    && "ACTIVE".equals(principal.getStatus());

                            return new AuthorizationDecision(isOwner && isActive);
                        })

                        // 자잘 에러 트랙 #9 (2026-05-23) — 라이더 배달 완료 사진 업로드
                        .requestMatchers("/api/delivery-photo/**").hasRole("RIDER")
                        .requestMatchers("/api/review-photo/**").hasRole("CUSTOMER")

                        // CUSTOMER 전용 (Phase 2-C에서 cart/order 코드 추가 시 활성화 예정)
                        .requestMatchers("/api/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/order/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/payment/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/notification/**").hasAnyRole("CUSTOMER", "OWNER")

                        // 2026-05-25 9건 트랙 #5/#6 — 챗봇은 CUSTOMER/OWNER/RIDER 모두 사용 (CS 챗봇 공용)
                        // MYPET 진입은 ChatbotService에서 CUSTOMER 체크 (사장/라이더 펫 자동 생성 차단)
                        .requestMatchers("/api/chatbot/**").authenticated()

                        // 2026-05-25 9건 트랙 #8 부채 Step B — 출석 시스템 (CUSTOMER 전용)
                        .requestMatchers("/api/attendance/**").hasRole("CUSTOMER")

                        // 2026-05-25 9건 트랙 #8 — 펫 도메인 (CUSTOMER 전용)
                        .requestMatchers("/api/pet/**").hasRole("CUSTOMER")

                        // 리뷰 작성/수정/삭제는 인증 (Phase 2-E에서 활성화 예정)
                        .requestMatchers(HttpMethod.POST, "/api/user/review/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/user/review/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/user/review/**").authenticated()

                        // 헬스/임시
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // 서버간 통신 internal API
                        .requestMatchers("/internal/**").permitAll()


                        .anyRequest().authenticated()
                )
                .build();
    }
}
