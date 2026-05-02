package com.green.mmg.auth.user;

import com.green.mmg.auth.user.model.UserGetRes;
import com.green.mmg.auth.user.model.UserSigninRes;
import com.green.mmg.auth.user.model.UserSignupReq;
import com.green.mmg.auth.user.model.UserUpdateReq;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.exception.GlobalExceptionHandler;
import com.green.mmg.common.model.JwtUser;
import com.green.mmg.common.model.UserPrincipal;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 통합 테스트.
 *
 * <p>학원 공유 DB 의존 회피: {@code standaloneSetup} + UserService Mockito mock.
 * GlobalExceptionHandler 등록으로 BusinessException/JwtException 응답 매핑 검증.
 * AuthenticationPrincipalArgumentResolver 등록 + SecurityContextHolder 직접 셋팅으로
 * @AuthenticationPrincipal 주입 흐름 재현.</p>
 *
 * <p>Security 필터(AT 검증/anyRequest().authenticated() 401 차단)는 풀 컨텍스트 영역이라
 * 본 테스트 범위 외 — Phase 2/3 통합 시나리오에서 별도 검증.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 통합 테스트 (standalone)")
class UserControllerTest {

    @Mock private UserService userService;

    @InjectMocks
    private UserController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(long userNo, String role, String name) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userNo, role, null, name));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/user/check-id")
    class CheckId {
        @Test
        @DisplayName("사용 가능 → 200 + '사용 가능합니다'")
        void available() throws Exception {
            when(userService.checkId("new-id")).thenReturn(true);

            mockMvc.perform(get("/api/user/check-id").param("userId", "new-id"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultMessage").value("사용 가능한 아이디입니다."))
                    .andExpect(jsonPath("$.resultData").doesNotExist());
        }

        @Test
        @DisplayName("중복 → 409 + '이미 사용 중'")
        void taken() throws Exception {
            when(userService.checkId("kjh")).thenReturn(false);

            mockMvc.perform(get("/api/user/check-id").param("userId", "kjh"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.resultMessage").value("이미 사용 중인 아이디입니다."));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/user/join")
    class Signup {
        @Test
        @DisplayName("200 + 응답에 userNo/name/role + 만료시각 포함")
        void happyPath() throws Exception {
            when(userService.signup(any(UserSignupReq.class), any(HttpServletResponse.class)))
                    .thenReturn(new UserSigninRes(100L, "준하", "CUSTOMER", 9999999999L, null));

            String body = """
                    {
                      "userId":"kjh",
                      "userPw":"plain-pw",
                      "name":"준하",
                      "birth":"1990-01-01",
                      "gender":1,
                      "tel":"010-1234-5678",
                      "role":"CUSTOMER"
                    }
                    """;

            mockMvc.perform(post("/api/user/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultMessage").value("회원가입 성공"))
                    .andExpect(jsonPath("$.resultData.userNo").value(100))
                    .andExpect(jsonPath("$.resultData.name").value("준하"))
                    .andExpect(jsonPath("$.resultData.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.resultData.atExpiresAt").value(9999999999L));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/user/login")
    class Signin {
        @Test
        @DisplayName("200 + 응답 JSON 일치")
        void happyPath() throws Exception {
            when(userService.signin(any(), any(HttpServletResponse.class)))
                    .thenReturn(new UserSigninRes(42L, "준하", "CUSTOMER", 9999999999L, null));

            mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"kjh\",\"userPw\":\"plain-pw\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultMessage").value("로그인 성공"))
                    .andExpect(jsonPath("$.resultData.userNo").value(42))
                    .andExpect(jsonPath("$.resultData.role").value("CUSTOMER"));
        }

        @Test
        @DisplayName("아이디/비번 불일치 → 401 + 메시지")
        void wrongCredentials_returns401() throws Exception {
            when(userService.signin(any(), any(HttpServletResponse.class)))
                    .thenThrow(new BusinessException("아이디 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED));

            mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"kjh\",\"userPw\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.resultMessage").value("아이디 또는 비밀번호가 틀렸습니다."));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/user/logout (Phase 4-C: principal userNo 위임)")
    class Signout {
        @Test
        @DisplayName("200 + signout(principal.userNo, res) 호출")
        void happyPath() throws Exception {
            authenticateAs(42L, "CUSTOMER", "준하");

            mockMvc.perform(post("/api/user/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultMessage").value("로그아웃 완료"));

            verify(userService).signout(eq(42L), any(HttpServletResponse.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/user/reissue (Step 1-A 핵심 검증)")
    class Reissue {
        @Test
        @DisplayName("200 + Service 위임 호출")
        void happyPath() throws Exception {
            doNothing().when(userService).reissue(any(HttpServletRequest.class), any(HttpServletResponse.class));

            mockMvc.perform(post("/api/user/reissue"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultMessage").value("AT 재발급 성공"));

            verify(userService).reissue(any(HttpServletRequest.class), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("RT 부재 → 401 (BusinessException)")
        void rtMissing_returns401() throws Exception {
            doThrow(new BusinessException("리프레시 토큰이 없습니다.", HttpStatus.UNAUTHORIZED))
                    .when(userService).reissue(any(HttpServletRequest.class), any(HttpServletResponse.class));

            mockMvc.perform(post("/api/user/reissue"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.resultMessage").value("리프레시 토큰이 없습니다."));
        }

        @Test
        @DisplayName("RT 만료 → 401 (JwtException → GlobalExceptionHandler.handleJwt) ★ Step 1-A 핵심")
        void rtExpired_returns401_notFiveHundred() throws Exception {
            doThrow(new ExpiredJwtException(null, null, "JWT expired"))
                    .when(userService).reissue(any(HttpServletRequest.class), any(HttpServletResponse.class));

            mockMvc.perform(post("/api/user/reissue"))
                    .andExpect(status().isUnauthorized())  // ★ 500 아닌 401 — Step 1-A 수정 검증
                    .andExpect(jsonPath("$.resultMessage").value("토큰이 만료됐거나 유효하지 않습니다."));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/user/me — @AuthenticationPrincipal")
    class GetMe {
        @Test
        @DisplayName("인증된 principal 셋팅 → 200 + signedUserNo 응답")
        void authenticated() throws Exception {
            authenticateAs(42L, "CUSTOMER", "준하");

            mockMvc.perform(get("/api/user/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultMessage").value("조회 성공"))
                    .andExpect(jsonPath("$.resultData.userNo").value(42))
                    .andExpect(jsonPath("$.resultData.name").value("준하"))
                    .andExpect(jsonPath("$.resultData.role").value("CUSTOMER"));
        }

        @Test
        @DisplayName("principal 미셋팅 (Security 필터 우회) → 200 + '로그인이 필요합니다' (Controller 방어 코드)")
        void noPrincipal_fallbackMessage() throws Exception {
            // 실 운영에선 Security 필터가 401로 차단하지만, standalone에서는 도달 가능 — 방어 분기 검증
            mockMvc.perform(get("/api/user/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultMessage").value("로그인이 필요합니다."));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/user — getUser")
    class GetUser {
        @Test
        @DisplayName("200 + Service에 principal.userNo 전달")
        void happyPath() throws Exception {
            authenticateAs(42L, "CUSTOMER", "준하");
            when(userService.getUser(42L))
                    .thenReturn(new UserGetRes("kjh", "준하", "010-1234-5678", 1, "1990-01-01"));

            mockMvc.perform(get("/api/user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultData.userId").value("kjh"))
                    .andExpect(jsonPath("$.resultData.name").value("준하"))
                    .andExpect(jsonPath("$.resultData.tel").value("010-1234-5678"))
                    .andExpect(jsonPath("$.resultData.gender").value(1))
                    .andExpect(jsonPath("$.resultData.birth").value("1990-01-01"));

            verify(userService).getUser(42L);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /api/user — updateUser")
    class UpdateUser {
        @Test
        @DisplayName("200 + Service에 principal.userNo + DTO 전달 (gender=0 명시 포함)")
        void happyPath() throws Exception {
            authenticateAs(42L, "CUSTOMER", "준하");

            String body = """
                    {
                      "name":"새이름",
                      "tel":"010-9999-9999",
                      "gender":0
                    }
                    """;

            mockMvc.perform(put("/api/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultMessage").value("수정 성공"));

            ArgumentCaptor<UserUpdateReq> captor = ArgumentCaptor.forClass(UserUpdateReq.class);
            verify(userService).updateUser(eq(42L), captor.capture());
            UserUpdateReq captured = captor.getValue();
            org.assertj.core.api.Assertions.assertThat(captured.getName()).isEqualTo("새이름");
            org.assertj.core.api.Assertions.assertThat(captured.getTel()).isEqualTo("010-9999-9999");
            org.assertj.core.api.Assertions.assertThat(captured.getGender()).isEqualTo(0);  // Integer 0 (null 아님)
        }

        @Test
        @DisplayName("body에 gender 필드 없음 → captured DTO의 gender == null (Step 1-B 검증)")
        void genderOmitted_isNull() throws Exception {
            authenticateAs(42L, "CUSTOMER", "준하");

            String body = """
                    {
                      "name":"부분수정"
                    }
                    """;

            mockMvc.perform(put("/api/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());

            ArgumentCaptor<UserUpdateReq> captor = ArgumentCaptor.forClass(UserUpdateReq.class);
            verify(userService).updateUser(eq(42L), captor.capture());
            // gender Integer (Step 1-B) — 미전송 시 null이어야 함
            org.assertj.core.api.Assertions.assertThat(captor.getValue().getGender()).isNull();
        }
    }
}
