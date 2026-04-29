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
    private final ConstJwt constJwt;

    // ── 아이디 중복확인
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
        jwtTokenManager.issue(res, jwtUser);

        return new UserSigninRes(saved.getUserNo(), saved.getName(), saved.getRole(),
                System.currentTimeMillis() + constJwt.getAccessTokenValidityMilliseconds(), null);
    }

    // ── 로그인
    public UserSigninRes signin(UserSigninReq req, HttpServletResponse res) {
        User user = userRepository.findByUserId(req.getUserId())
                .orElseThrow(() -> new BusinessException("아이디 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED));
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
        if (req.getGender() != 0)                                user.setGender(req.getGender());
        if (req.getBirth() != null && !req.getBirth().isBlank()) user.setBirth(req.getBirth());
        if (req.getUserPw() != null && !req.getUserPw().isBlank())
            user.setUserPw(passwordEncoder.encode(req.getUserPw()));
    }
}
