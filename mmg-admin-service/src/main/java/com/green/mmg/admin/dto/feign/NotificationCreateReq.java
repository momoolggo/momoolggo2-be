package com.green.mmg.admin.dto.feign;

public record NotificationCreateReq(
        Long userNo,
        String notificationType,
        String title,
        String content,
        String targetUrl
) {
    public static NotificationCreateReq reviewBlind(Long userNo, Long blindId, String aiReason) {
        return new NotificationCreateReq(
                userNo,
                "REVIEW_BLIND",
                "작성하신 리뷰가 블라인드 처리되었습니다",
                "AI 심사 결과 규정 위반(" + aiReason + ")으로 리뷰가 블라인드 처리되었습니다. " +
                "이의가 있으시면 소명을 요청하실 수 있습니다.",
                "/my/reviews/blind/" + blindId
        );
    }
}