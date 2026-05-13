package com.green.mmg.rider.rider.model;

/**
 * 라이더 프로필 응답 dto — PUT /api/rider/profile, GET /api/rider/me 공통.
 *
 * <p>응답 동결 (CLAUDE.md §6 규칙 7) — 필드 추가 OK, 제거/이름 변경 금지.</p>
 *
 * <p>{@code accountNo}는 본인 조회 한정 노출 — Phase 6+ 마스킹 검토 (D7 손님 전화번호 패턴 일관).</p>
 */
public record RiderProfileRes(
        Long riderNo,
        Long userNo,
        String status,          // RiderStatus enum 직렬화 (PENDING/ACTIVE/EATING/SUSPENDED)
        String licenseNo,
        String licenseType,
        String vehicleType,
        String accountBank,
        String accountNo,
        String accountHolder
) {
    public static RiderProfileRes from(Rider rider) {
        return new RiderProfileRes(
                rider.getRiderNo(),
                rider.getUserNo(),
                rider.getStatus().name(),
                rider.getLicenseNo(),
                rider.getLicenseType(),
                rider.getVehicleType().name(),
                rider.getAccountBank(),
                rider.getAccountNo(),
                rider.getAccountHolder()
        );
    }
}
