package com.green.mmg.auth.user;

import com.green.mmg.auth.token.RefreshTokenStore;
import com.green.mmg.auth.user.model.*;
import com.green.mmg.common.constants.ConstJwt;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.jwt.JwtTokenManager;
import com.green.mmg.common.jwt.JwtTokenProvider;
import com.green.mmg.common.model.JwtUser;
import com.green.mmg.common.util.MyCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Phase 3-A: MyBatis → JPA 전환 (단순 CRUD).
 * UserMapper 제거됨 — 모든 쿼리 UserRepository로 이동.
 * Phase 3-B 이후 복잡 쿼리 필요시 UserMapper.java + User.xml 재도입 가능 (mybatis starter 영구 유지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenManager jwtTokenManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final MyCookieUtil myCookieUtil;
    private final ConstJwt constJwt;
    private final RefreshTokenStore refreshTokenStore;  // Phase 4-C: RT revoke 가능성 보장

    // ── 아이디 중복확인
    @Transactional(readOnly = true)
    public boolean checkId(String userId) {
        return !userRepository.existsByUserId(userId);
    }

    // ── 회원가입 (옵션 D-1: BFF 패턴)
    // 가입 후 즉시 AT/RT 발급. user_address 등록은 프론트가 별도 POST /api/address 호출 (main-service).
    @Transactional
    public UserSigninRes signup(UserSignupReq req, HttpServletResponse res) {
        String role = (req.getRole() == null || req.getRole().isBlank()) ? "CUSTOMER" : req.getRole();

        User user = new User();
        user.setUserId(req.getUserId());
        user.setUserPw(passwordEncoder.encode(req.getUserPw()));
        user.setName(req.getName());
        user.setBirth(req.getBirth());
        user.setGender(req.getGender() == null ? 0 : req.getGender());
        user.setTel(req.getTel());
        user.setRole(role);

        User saved = userRepository.save(user);

        JwtUser jwtUser = new JwtUser(saved.getUserNo(), saved.getRole(), null, saved.getName());
        issueAndStoreTokens(res, jwtUser);

        return new UserSigninRes(saved.getUserNo(), saved.getName(), saved.getRole(),
                System.currentTimeMillis() + constJwt.getAccessTokenValidityMilliseconds(), null);
    }

    // ── 로그인 (조회 + JWT 발급, DB 변경 없음)
    @Transactional(readOnly = true)
    public UserSigninRes signin(UserSigninReq req, HttpServletResponse res) {
        User user = userRepository.findByUserId(req.getUserId())
                .orElseThrow(() -> new BusinessException("아이디 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(req.getUserPw(), user.getUserPw())) {
            throw new BusinessException("아이디 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED);
        }
        JwtUser jwtUser = new JwtUser(user.getUserNo(), user.getRole(), null, user.getName());
        issueAndStoreTokens(res, jwtUser);
        return new UserSigninRes(user.getUserNo(), user.getName(), user.getRole(),
                System.currentTimeMillis() + constJwt.getAccessTokenValidityMilliseconds(), null);
    }

    /**
     * Phase 4-C: AT/RT 발급 + RT를 Redis에 저장. signup/signin 공통.
     *
     * <p>순서: 쿠키 세팅 → Redis 저장 (사용자 결정 — Redis 좀비 데이터 회피).
     * Redis 저장 실패(RedisConnectionFailureException 등) 시 그대로 throw —
     * D1 결정(정합성 우선): login 자체 실패. best-effort 회피.</p>
     */
    private void issueAndStoreTokens(HttpServletResponse res, JwtUser jwtUser) {
        String accessToken = jwtTokenProvider.generateAccessToken(jwtUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtUser);
        jwtTokenManager.setAccessTokenInCookie(res, accessToken);
        jwtTokenManager.setRefreshTokenInCookie(res, refreshToken);
        refreshTokenStore.save(jwtUser.getSignedUserNo(), refreshToken,
                Duration.ofMillis(constJwt.getRefreshTokenValidityMilliseconds()));
    }

    // ── 로그아웃
    public void signout(HttpServletResponse res) {
        jwtTokenManager.signOut(res);
    }

    // ── AT 재발급 (RT 검증 후 새 AT만 발급, RT는 그대로)
    // RT 부재 → BusinessException(401) / RT 만료·위변조 → JwtException → GlobalExceptionHandler가 401로 응답
    // Phase 4-C: 저장소 RT 비교 추가 — signout 후 또는 위조 시도 시 401 (revoke 가능성 보장)
    public void reissue(HttpServletRequest req, HttpServletResponse res) {
        String cookieRefreshToken = myCookieUtil.getValue(req, constJwt.getRefreshTokenCookieName());
        if (cookieRefreshToken == null) {
            throw new BusinessException("리프레시 토큰이 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        JwtUser jwtUser = jwtTokenProvider.getJwtUserFromToken(cookieRefreshToken);

        // Phase 4-C: 저장 RT == 쿠키 RT 비교. 부재(signout 후)/불일치(위조) 시 401, 재로그인 강제.
        String storedRefreshToken = refreshTokenStore.get(jwtUser.getSignedUserNo())
                .orElseThrow(() -> new BusinessException(
                        "리프레시 토큰이 만료되었거나 로그아웃되었습니다.", HttpStatus.UNAUTHORIZED));
        if (!cookieRefreshToken.equals(storedRefreshToken)) {
            throw new BusinessException(
                    "리프레시 토큰이 유효하지 않습니다. 재로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }

        jwtTokenManager.setAccessTokenInCookie(res, jwtUser);
    }

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserGetRes getUser(Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new BusinessException("회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return new UserGetRes(user.getUserId(), user.getName(), user.getTel(), user.getGender(), user.getBirth());
    }

    // 내 정보 수정 — JPA dirty checking (기존 MyBatis <if> 동적 UPDATE와 동일 의미)
    @Transactional
    public void updateUser(Long userNo, UserUpdateReq req) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new BusinessException("회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (req.getName() != null && !req.getName().isBlank())  user.setName(req.getName());
        if (req.getTel() != null && !req.getTel().isBlank())    user.setTel(req.getTel());
        if (req.getGender() != null)                             user.setGender(req.getGender());
        if (req.getBirth() != null && !req.getBirth().isBlank()) user.setBirth(req.getBirth());
        if (req.getUserPw() != null && !req.getUserPw().isBlank())
            user.setUserPw(passwordEncoder.encode(req.getUserPw()));
    }
}
