package com.green.mmg.auth.user.model;

import lombok.Getter;
import lombok.Setter;

// DB의 user 테이블과 1:1로 대응되는 엔티티 클래스
// MyBatis가 SELECT 결과를 이 객체에 자동으로 담아줌
// ⚠️ userPw(암호화된 비밀번호)가 있어서 절대 프론트에 직접 반환하면 안 됨
@Getter
@Setter
public class User {
    private long   userNo;   // PK (DB: user_no)
    private String userId;   // 로그인 아이디 (DB: user_id)
    private String userPw;   // BCrypt로 암호화된 비밀번호 (DB: user_pw)
    private String role;     // 권한 (DB: role)
    private String name;     // 이름 (DB: name)
    private String birth;    // 생년월일 (DB: birth)
    private int    gender;   // 성별 (DB: gender)
    private int    green;    // 친환경점수 (DB: green)
    private int    kind;     // 주문 온도 (DB: kind)
    private String rank;     // 멤버등급 (DB: rank)
    private String tel;      // 휴대폰번호 (DB: tel)
}
