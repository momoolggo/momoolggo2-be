package com.green.mmg.common.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Internal API: GET /internal/auth/user/{userNo} 응답 DTO.
 * Phase 4-A에서 main → auth cross-schema 조회 시 사용.
 *
 * address 필드는 항상 빈 문자열 — main-service가 자체 user_address 테이블 조회로 채움.
 * (Phase 1-B-3.5 ERD 준수 — address 테이블은 my_mmg_main 소속)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBriefDto {
    private long userNo;
    private String name;
    private String tel;
    private String address;   // 항상 "" — main이 자체 채움
}
