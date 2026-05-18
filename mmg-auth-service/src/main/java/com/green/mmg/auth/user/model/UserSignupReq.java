package com.green.mmg.auth.user.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignupReq {
    private String name;
    private Long userNo;   // useGeneratedKeys로 INSERT 후 자동 채워짐 (소문자 시작 필수)
    private String userId;
    private String userPw;
    private Integer gender;
    private String birth;
    private String tel;
    private String role;
    private String address;
    private String addressDetail;
    private Double lat;
    private Double lng;

    // 작업 C (2026-05-18): 이용약관 동의. true 아니면 가입 차단 (이력 저장 X).
    private Boolean agreedToTerms;
}
