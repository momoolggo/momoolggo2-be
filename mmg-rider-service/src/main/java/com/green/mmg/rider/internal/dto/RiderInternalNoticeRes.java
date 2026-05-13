package com.green.mmg.rider.internal.dto;

/**
 * Admin → Rider 공지 작성 응답 — POST /internal/rider/notice.
 * 사용자 박제 형식 일관 ({@code resultMessage} / {@code resultData}).
 * GlobalExceptionHandler ResultResponse 패턴 추종 (mmg-common 정착, R3-a 정정 일관).
 */
public record RiderInternalNoticeRes(
        String resultMessage,
        Object resultData
) {
    public static RiderInternalNoticeRes success() {
        return new RiderInternalNoticeRes("공지 발송 완료", null);
    }
}
