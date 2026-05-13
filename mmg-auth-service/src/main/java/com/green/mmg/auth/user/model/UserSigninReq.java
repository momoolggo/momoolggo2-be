package com.green.mmg.auth.user.model;

import lombok.Getter;
import lombok.Setter;

// 로그인 요청 시 프론트에서 넘어오는 데이터를 담는 DTO
// POST /api/user/login
// @RequestBody 로 JSON → 이 객체로 자동 변환됨
@Getter
@Setter
public class UserSigninReq {
    private String userId;  // 로그인 아이디 (DB: user_id)
    private String userPw;  // 비밀번호 평문 (DB: user_pw) → 서비스에서 DB 암호화값과 비교
}
