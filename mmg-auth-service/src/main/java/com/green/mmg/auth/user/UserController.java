package com.green.mmg.auth.user;

import com.green.mmg.auth.user.model.*;
import com.green.mmg.common.constants.ConstJwt;
import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.jwt.JwtTokenManager;
import com.green.mmg.common.jwt.JwtTokenProvider;
import com.green.mmg.common.model.JwtUser;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.common.util.MyCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MyCookieUtil myCookieUtil;
    private final ConstJwt constJwt;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenManager jwtTokenManager;


    // ── 아이디 중복확인 GET /api/user/check-id?userId=xxx
    @GetMapping("/check-id")
    public ResultResponse<Void> checkId(@RequestParam String userId) {
        boolean available = userService.checkId(userId);
        if (!available) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        return new ResultResponse<>("사용 가능한 아이디입니다.", null);
    }

    // ── 회원가입 POST /api/user/join
    @PostMapping("/join")
    public ResultResponse<Void> signup(@RequestBody UserSignupReq req) {
        userService.signup(req);
        return new ResultResponse<>("회원가입 성공", null);
    }

    // ── 로그인 POST /api/user/login
    @PostMapping("/login")
    public ResultResponse<UserSigninRes> signin(@RequestBody UserSigninReq req,
                                                HttpServletResponse res) {
        UserSigninRes data = userService.signin(req, res);
        return new ResultResponse<>("로그인 성공", data);
    }

    // ── 로그아웃 POST /api/user/logout
    @PostMapping("/logout")
    public ResultResponse<Void> signout(HttpServletResponse res) {
        userService.signout(res);
        return new ResultResponse<>("로그아웃 완료", null);
    }

    // ── 내 정보 조회 GET /api/user/me
    @GetMapping("/me")
    public ResultResponse<UserSigninRes> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return new ResultResponse<>("로그인이 필요합니다.", null);
        }
        UserSigninRes data = new UserSigninRes(
                principal.getSignedUserNo(),
                principal.getName(),
                principal.getRole(),
                0L,  // ← me는 만료시각 안 씀, 0으로 채우기
                null
        );
        return new ResultResponse<>("조회 성공", data);
    }

    // 내 정보 조회
    @GetMapping
    public ResultResponse<UserGetRes> getUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserGetRes data = userService.getUser(principal.getSignedUserNo());
        return new ResultResponse<>("조회 성공", data);
    }

    // 내 정보 수정
    @PutMapping
    public ResultResponse<Void> updateUser(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestBody UserUpdateReq req) {
        userService.updateUser(principal.getSignedUserNo(), req);
        return new ResultResponse<>("수정 성공", null);
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest req, HttpServletResponse res) {
        // RT 쿠키에서 꺼내기
        String refreshToken = myCookieUtil.getValue(req, constJwt.getRefreshTokenCookieName());
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(new ResultResponse<>("RT 없음", null));
        }

        // RT 검증 & JwtUser 추출
        JwtUser jwtUser = jwtTokenProvider.getJwtUserFromToken(refreshToken);

        // 새 AT만 발급 (RT는 그대로)
        jwtTokenManager.setAccessTokenInCookie(res, jwtUser);

        return ResponseEntity.ok(new ResultResponse<>("AT 재발급 성공", null));
    }

    // 리뷰 엔드포인트는 Phase 2에서 main-service에 작성
}
