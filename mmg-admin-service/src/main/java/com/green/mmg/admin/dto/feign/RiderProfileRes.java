package com.green.mmg.admin.dto.feign;

/**
 * admin ← rider Feign: 라이더 프로필 응답 — interfaces.md §3.5 (Group 8 신설 2026-05-17, Q-A1 (라++)).
 *
 * <p>rider 측 {@code com.green.mmg.rider.rider.model.RiderProfileRes} 9 필드 1:1 일관 (case-#34 영역 분리 강제).
 * {@code status}: String으로 박제 (rider 측 enum 4값 PENDING/ACTIVE/EATING/SUSPENDED 직렬화).
 * admin SettlementsStatus enum과 이름 충돌 회피 (case-#34 일관).</p>
 */
public record RiderProfileRes(
        Long riderNo,
        Long userNo,
        String status,
        String licenseNo,
        String licenseType,
        String vehicleType,
        String accountBank,
        String accountNo,
        String accountHolder
) {
}
