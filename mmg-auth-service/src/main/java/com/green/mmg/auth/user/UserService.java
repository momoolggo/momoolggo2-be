package com.green.mmg.auth.user;

import com.green.mmg.auth.address.UserAddressMapper;
import com.green.mmg.auth.address.model.UserAddressReq;
import com.green.mmg.auth.user.model.*;
import com.green.mmg.common.constants.ConstJwt;
import com.green.mmg.common.jwt.JwtTokenManager;
import com.green.mmg.common.model.JwtUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final UserAddressMapper userAddressMapper;
    private final ConstJwt constJwt;

    // ── 아이디 중복확인
    public boolean checkId(String userId) {
        return userMapper.countByUserId(userId) == 0;
    }

    // ── 회원가입
    @Transactional
    public void signup(UserSignupReq req) {
        req.setUserPw(passwordEncoder.encode(req.getUserPw()));
        if (req.getRole() == null || req.getRole().isBlank()) {
            req.setRole("CUSTOMER");
        }
        userMapper.signup(req);

        // 주소가 있으면 address 테이블에도 저장
        if (req.getAddress() != null && !req.getAddress().isBlank()) {
            UserAddressReq addressReq = new UserAddressReq();
            addressReq.setUserNo(req.getUserNo());
            addressReq.setAddress(req.getAddress());
            addressReq.setAddressDetail(req.getAddressDetail());
            addressReq.setLat(req.getLat());
            addressReq.setLng(req.getLng());
            addressReq.setDefaultAd(1);
            userAddressMapper.save(addressReq);
        }
    }

    // ── 로그인
    public UserSigninRes signin(UserSigninReq req, HttpServletResponse res) {
        User user = userMapper.findByUserId(req.getUserId());
        if (user == null) {
            throw new RuntimeException("아이디 또는 비밀번호가 틀렸습니다.");
        }
        if (!passwordEncoder.matches(req.getUserPw(), user.getUserPw())) {
            throw new RuntimeException("아이디 또는 비밀번호가 틀렸습니다.");
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

    // ========== 리뷰 관련 메서드는 Phase 2에서 main-service에 신규 작성 ==========
}
