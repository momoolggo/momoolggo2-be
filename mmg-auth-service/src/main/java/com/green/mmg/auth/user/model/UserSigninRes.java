package com.green.mmg.auth.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 로그인 성공 시 프론트로 반환하는 DTO
// ⚠️ userPw 같은 민감한 정보는 절대 여기 담으면 안 됨
@Getter
@AllArgsConstructor  // 모든 필드를 파라미터로 받는 생성자 자동 생성
public class UserSigninRes {
    private long   userNo;  // 로그인한 유저의 PK
    private String name;    // 이름
    private String role;    // 역할 (프론트에서 화면 분기에 사용)
    private long   atExpiresAt;
    private String storeName;
}
