package com.green.mmg.rider.rider.model;

/**
 * PUT /api/rider/profile 요청 dto.
 *
 * <p>⚠️ {@code userNo} 필드 두지 않음 — callerUserNo는 SecurityContextHolder에서 추출.
 * 클라이언트 dto 신뢰 X (위조 방지 패턴, feedback_dto_userno_forgery.md).</p>
 *
 * <p>검증은 RiderService 진입부에서 명시 (auth/main 기존 패턴 일관, validation starter 미도입).</p>
 */
public record RiderProfileReq(
        String licenseNo,
        String licenseType,
        String vehicleType,
        String accountBank,
        String accountNo,
        String accountHolder,
        String phone,
        String licenseImageUrl   // 2026-05-28 트랙 — 가입 시 면허증 사진 URL (필수, FE에서 /api/rider-license/upload 호출 후 전달)
) {
}
