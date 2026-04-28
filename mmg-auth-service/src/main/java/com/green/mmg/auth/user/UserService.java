package com.green.mmg.auth.user;

import com.green.mmg.auth.user.model.*;
import com.green.mmg.common.constants.ConstJwt;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.jwt.JwtTokenManager;
import com.green.mmg.common.model.JwtUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenManager jwtTokenManager;
    private final ConstJwt constJwt;

    // ── 아이디 중복확인
    public boolean checkId(String userId) {
        return userMapper.countByUserId(userId) == 0;
    }

    // ── 회원가입 (옵션 D-1: BFF 패턴)
    // 회원가입 후 즉시 AT/RT 발급. user_address 등록은 프론트가 별도 POST /api/address 호출 (main-service).
    // UserSignupReq.address* 필드는 forward compatibility를 위해 유지하되 사용 안 함.
    @Transactional
    public UserSigninRes signup(UserSignupReq req, HttpServletResponse res) {
        req.setUserPw(passwordEncoder.encode(req.getUserPw()));
        if (req.getRole() == null || req.getRole().isBlank()) {
            req.setRole("CUSTOMER");
        }
        userMapper.signup(req);   // useGeneratedKeys → req.userNo 자동 채워짐

        // 가입 직후 즉시 인증 (옵션 D-1) — req에 모든 정보 있으니 추가 fetch 불필요
        long userNo = req.getUserNo();
        String role = req.getRole();
        String name = req.getName();

        JwtUser jwtUser = new JwtUser(userNo, role, null, name);
        jwtTokenManager.issue(res, jwtUser);

        return new UserSigninRes(userNo, name, role,
                System.currentTimeMillis() + constJwt.getAccessTokenValidityMilliseconds(), null);
    }

    // ── 로그인
    public UserSigninRes signin(UserSigninReq req, HttpServletResponse res) {
        User user = userMapper.findByUserId(req.getUserId());
        if (user == null) {
            throw new BusinessException("아이디 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(req.getUserPw(), user.getUserPw())) {
            throw new BusinessException("아이디 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED);
        }
        JwtUser jwtUser = new JwtUser(user.getUserNo(), user.getRole(), null, user.getName());
        jwtTokenManager.issue(res, jwtUser);
        return new UserSigninRes(user.getUserNo(), user.getName(), user.getRole(),
                System.currentTimeMillis() + constJwt.getAccessTokenValidityMilliseconds(), null);
    }

    // ── 로그아웃
    public void signout(HttpServletResponse res) {
        jwtTokenManager.signOut(res);
    }

    // 내 정보 조회
    public UserGetRes getUser(Long userNo) {
        User user = userMapper.findByUserNo(userNo);
        return new UserGetRes(user.getUserId(), user.getName(), user.getTel(), user.getGender(), user.getBirth());
    }

    // 내 정보 수정
    public void updateUser(Long userNo, UserUpdateReq req) {
        if (req.getUserPw() != null && !req.getUserPw().isBlank()) {
            req.setUserPw(passwordEncoder.encode(req.getUserPw()));
        }
        req.setUserNo(userNo);
        userMapper.update(req);
    }

    // ========== 리뷰 관련 메서드는 Phase 2-E에서 main-service에 신규 작성 ==========
}
