package com.green.mmg.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// JWT 토큰 안에 담을 유저 정보
// 로그인 시 이 객체를 JSON으로 직렬화해서 토큰에 넣고,
// 요청마다 토큰에서 꺼내서 역직렬화함
// ⚠️ 민감한 정보(비밀번호 등)는 절대 여기 담으면 안 됨
@Getter
@AllArgsConstructor
@NoArgsConstructor   // ObjectMapper 역직렬화 시 필요 (빈 생성자로 객체 먼저 만들고 값을 채움)
public class JwtUser {
    private long   signedUserNo;  // 로그인한 유저의 PK (user_no)
    private String role;          // CUSTOMER / OWNER / RIDER / ADMIN
    private String status;        // ACTIVE / PENDING / BLOCKED (현재 미사용, 확장용)
    private String name;
}
