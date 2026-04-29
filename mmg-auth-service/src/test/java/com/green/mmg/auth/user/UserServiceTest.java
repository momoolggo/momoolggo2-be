package com.green.mmg.auth.user;

import com.green.mmg.auth.user.model.*;
import com.green.mmg.common.constants.ConstJwt;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.jwt.JwtTokenManager;
import com.green.mmg.common.jwt.JwtTokenProvider;
import com.green.mmg.common.model.JwtUser;
import com.green.mmg.common.util.MyCookieUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenManager jwtTokenManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private MyCookieUtil myCookieUtil;
    @Mock private ConstJwt constJwt;
    @Mock private HttpServletRequest httpReq;
    @Mock private HttpServletResponse httpRes;

    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setUserNo(42L);
        existingUser.setUserId("kjh");
        existingUser.setUserPw("$2a$10$encoded-hash");
        existingUser.setName("준하");
        existingUser.setRole("CUSTOMER");
        existingUser.setBirth("1990-01-01");
        existingUser.setGender(1);
        existingUser.setTel("010-1234-5678");
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("checkId — 아이디 중복확인")
    class CheckId {
        @Test
        @DisplayName("DB에 없는 ID → true (사용 가능)")
        void available() {
            when(userRepository.existsByUserId("new-id")).thenReturn(false);
            assertThat(userService.checkId("new-id")).isTrue();
        }

        @Test
        @DisplayName("DB에 있는 ID → false (사용 불가)")
        void taken() {
            when(userRepository.existsByUserId("kjh")).thenReturn(true);
            assertThat(userService.checkId("kjh")).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("signup — 회원가입 (BFF: 즉시 AT/RT 발급)")
    class Signup {
        @Test
        @DisplayName("happy path: User 저장 + 비번 BCrypt 인코딩 + JWT 발급")
        void happyPath() {
            UserSignupReq req = newSignupReq();
            req.setRole("CUSTOMER");
            when(passwordEncoder.encode("plain-pw")).thenReturn("encoded-pw");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserNo(100L);
                return u;
            });
            when(constJwt.getAccessTokenValidityMilliseconds()).thenReturn(60_000L);

            UserSigninRes res = userService.signup(req, httpRes);

            ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(savedCaptor.capture());
            User saved = savedCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo("kjh");
            assertThat(saved.getUserPw()).isEqualTo("encoded-pw");  // BCrypt 결과
            assertThat(saved.getUserPw()).isNotEqualTo("plain-pw"); // 평문 미저장
            assertThat(saved.getName()).isEqualTo("준하");
            assertThat(saved.getRole()).isEqualTo("CUSTOMER");
            assertThat(saved.getGender()).isEqualTo(1);

            verify(jwtTokenManager).issue(eq(httpRes), any(JwtUser.class));

            assertThat(res.getUserNo()).isEqualTo(100L);
            assertThat(res.getName()).isEqualTo("준하");
            assertThat(res.getRole()).isEqualTo("CUSTOMER");
            assertThat(res.getAtExpiresAt()).isGreaterThan(System.currentTimeMillis());
        }

        @Test
        @DisplayName("role null → 기본값 CUSTOMER 저장")
        void roleNull_defaultsToCustomer() {
            UserSignupReq req = newSignupReq();
            req.setRole(null);
            when(passwordEncoder.encode(any())).thenReturn("x");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(constJwt.getAccessTokenValidityMilliseconds()).thenReturn(0L);

            userService.signup(req, httpRes);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo("CUSTOMER");
        }

        @Test
        @DisplayName("gender null → 0으로 저장")
        void genderNull_defaultsToZero() {
            UserSignupReq req = newSignupReq();
            req.setGender(null);
            when(passwordEncoder.encode(any())).thenReturn("x");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(constJwt.getAccessTokenValidityMilliseconds()).thenReturn(0L);

            userService.signup(req, httpRes);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getGender()).isEqualTo(0);
        }

        private UserSignupReq newSignupReq() {
            UserSignupReq req = new UserSignupReq();
            req.setUserId("kjh");
            req.setUserPw("plain-pw");
            req.setName("준하");
            req.setBirth("1990-01-01");
            req.setGender(1);
            req.setTel("010-1234-5678");
            return req;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("signin — 로그인")
    class Signin {
        @Test
        @DisplayName("happy path: JWT 발급 + 응답에 userNo/role/이름 포함")
        void happyPath() {
            UserSigninReq req = new UserSigninReq();
            req.setUserId("kjh");
            req.setUserPw("plain-pw");
            when(userRepository.findByUserId("kjh")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("plain-pw", existingUser.getUserPw())).thenReturn(true);
            when(constJwt.getAccessTokenValidityMilliseconds()).thenReturn(60_000L);

            UserSigninRes res = userService.signin(req, httpRes);

            verify(jwtTokenManager).issue(eq(httpRes), any(JwtUser.class));
            assertThat(res.getUserNo()).isEqualTo(42L);
            assertThat(res.getRole()).isEqualTo("CUSTOMER");
            assertThat(res.getName()).isEqualTo("준하");
        }

        @Test
        @DisplayName("존재하지 않는 userId → BusinessException(401)")
        void userNotFound_throws401() {
            UserSigninReq req = new UserSigninReq();
            req.setUserId("ghost");
            req.setUserPw("anything");
            when(userRepository.findByUserId("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.signin(req, httpRes))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("아이디 또는 비밀번호")
                    .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);

            verify(jwtTokenManager, never()).issue(any(), any());
        }

        @Test
        @DisplayName("비밀번호 불일치 → BusinessException(401)")
        void wrongPassword_throws401() {
            UserSigninReq req = new UserSigninReq();
            req.setUserId("kjh");
            req.setUserPw("wrong-pw");
            when(userRepository.findByUserId("kjh")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("wrong-pw", existingUser.getUserPw())).thenReturn(false);

            assertThatThrownBy(() -> userService.signin(req, httpRes))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("아이디 또는 비밀번호")
                    .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);

            verify(jwtTokenManager, never()).issue(any(), any());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("signout — 로그아웃")
    class Signout {
        @Test
        @DisplayName("JwtTokenManager.signOut() 호출")
        void delegatesToManager() {
            userService.signout(httpRes);
            verify(jwtTokenManager).signOut(httpRes);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("reissue — AT 재발급 (Step 1-A 핵심 검증)")
    class Reissue {
        @Test
        @DisplayName("happy path: RT 검증 후 새 AT만 쿠키에 셋팅 (RT는 그대로)")
        void happyPath() {
            JwtUser jwtUser = new JwtUser(42L, "CUSTOMER", null, "준하");
            when(constJwt.getRefreshTokenCookieName()).thenReturn("refresh-token");
            when(myCookieUtil.getValue(httpReq, "refresh-token")).thenReturn("valid-rt");
            when(jwtTokenProvider.getJwtUserFromToken("valid-rt")).thenReturn(jwtUser);

            userService.reissue(httpReq, httpRes);

            verify(jwtTokenManager).setAccessTokenInCookie(httpRes, jwtUser);
            verify(jwtTokenManager, never()).issue(any(), any());  // RT 재발급 X
        }

        @Test
        @DisplayName("RT 쿠키 없음 → BusinessException(401)")
        void rtMissing_throws401() {
            when(constJwt.getRefreshTokenCookieName()).thenReturn("refresh-token");
            when(myCookieUtil.getValue(httpReq, "refresh-token")).thenReturn(null);

            assertThatThrownBy(() -> userService.reissue(httpReq, httpRes))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리프레시 토큰이 없습니다")
                    .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);

            verifyNoInteractions(jwtTokenProvider);
            verify(jwtTokenManager, never()).setAccessTokenInCookie(any(HttpServletResponse.class), any(JwtUser.class));
        }

        @Test
        @DisplayName("RT 만료 → ExpiredJwtException 그대로 전파 (GlobalExceptionHandler가 401로 변환)")
        void rtExpired_propagatesJwtException() {
            when(constJwt.getRefreshTokenCookieName()).thenReturn("refresh-token");
            when(myCookieUtil.getValue(httpReq, "refresh-token")).thenReturn("expired-rt");
            when(jwtTokenProvider.getJwtUserFromToken("expired-rt"))
                    .thenThrow(new ExpiredJwtException(null, null, "expired"));

            assertThatThrownBy(() -> userService.reissue(httpReq, httpRes))
                    .isInstanceOf(ExpiredJwtException.class)
                    .isInstanceOf(JwtException.class);  // GlobalExceptionHandler.handleJwt가 잡음

            verify(jwtTokenManager, never()).setAccessTokenInCookie(any(HttpServletResponse.class), any(JwtUser.class));
        }

        @Test
        @DisplayName("RT 위변조 → SignatureException(JwtException 하위) 그대로 전파")
        void rtTampered_propagatesJwtException() {
            when(constJwt.getRefreshTokenCookieName()).thenReturn("refresh-token");
            when(myCookieUtil.getValue(httpReq, "refresh-token")).thenReturn("tampered-rt");
            when(jwtTokenProvider.getJwtUserFromToken("tampered-rt"))
                    .thenThrow(new SignatureException("bad signature"));

            assertThatThrownBy(() -> userService.reissue(httpReq, httpRes))
                    .isInstanceOf(JwtException.class);

            verify(jwtTokenManager, never()).setAccessTokenInCookie(any(HttpServletResponse.class), any(JwtUser.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getUser — 내 정보 조회")
    class GetUser {
        @Test
        @DisplayName("happy path: 5개 필드 매핑")
        void happyPath() {
            when(userRepository.findById(42L)).thenReturn(Optional.of(existingUser));

            UserGetRes res = userService.getUser(42L);

            assertThat(res.getUserId()).isEqualTo("kjh");
            assertThat(res.getName()).isEqualTo("준하");
            assertThat(res.getTel()).isEqualTo("010-1234-5678");
            assertThat(res.getGender()).isEqualTo(1);
            assertThat(res.getBirth()).isEqualTo("1990-01-01");
        }

        @Test
        @DisplayName("존재하지 않는 userNo → BusinessException(404)")
        void notFound_throws404() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("회원 정보를 찾을 수 없습니다")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateUser — 내 정보 수정 (Step 1-B 핵심 검증)")
    class UpdateUser {
        @Test
        @DisplayName("일부 필드만 변경: name/tel만 수정, 나머지 보존")
        void partialUpdate() {
            when(userRepository.findById(42L)).thenReturn(Optional.of(existingUser));
            UserUpdateReq req = new UserUpdateReq();
            req.setName("새이름");
            req.setTel("010-9999-9999");
            // gender/birth/userPw 미설정

            userService.updateUser(42L, req);

            assertThat(existingUser.getName()).isEqualTo("새이름");
            assertThat(existingUser.getTel()).isEqualTo("010-9999-9999");
            assertThat(existingUser.getGender()).isEqualTo(1);          // 그대로
            assertThat(existingUser.getBirth()).isEqualTo("1990-01-01"); // 그대로
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("gender=0 명시 전송 → 0으로 변경됨 (Integer 타입 수정 핵심)")
        void genderZero_actuallyChanges() {
            when(userRepository.findById(42L)).thenReturn(Optional.of(existingUser));
            UserUpdateReq req = new UserUpdateReq();
            req.setGender(0);  // 명시적으로 0 전송

            userService.updateUser(42L, req);

            assertThat(existingUser.getGender()).isEqualTo(0);  // 변경 반영
        }

        @Test
        @DisplayName("gender null (미전송) → gender 변경 안 됨")
        void genderNull_unchanged() {
            when(userRepository.findById(42L)).thenReturn(Optional.of(existingUser));
            UserUpdateReq req = new UserUpdateReq();
            req.setGender(null);  // 미전송

            userService.updateUser(42L, req);

            assertThat(existingUser.getGender()).isEqualTo(1);  // 기존 값 보존
        }

        @Test
        @DisplayName("userPw 변경 → BCrypt 인코딩 후 저장")
        void passwordChange_encoded() {
            when(userRepository.findById(42L)).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.encode("new-pw")).thenReturn("$2a$10$new-encoded");
            UserUpdateReq req = new UserUpdateReq();
            req.setUserPw("new-pw");

            userService.updateUser(42L, req);

            assertThat(existingUser.getUserPw()).isEqualTo("$2a$10$new-encoded");
            verify(passwordEncoder).encode("new-pw");
        }

        @Test
        @DisplayName("존재하지 않는 userNo → BusinessException(404)")
        void notFound_throws404() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            UserUpdateReq req = new UserUpdateReq();
            req.setName("anything");

            assertThatThrownBy(() -> userService.updateUser(999L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
