package com.green.mmg.admin.dto.feign;

public record CustomerNotificationReq(
        Long userNo,
        String notificationType,
        String title,
        String content,
        String targetUrl
) {
    public static CustomerNotificationReq notice(String noticeTitle) {
        return new CustomerNotificationReq(
                null,
                "NOTICE",
                "새 공지사항이 등록되었습니다.",
                noticeTitle,
                "/notice"
        );
    }

    public static CustomerNotificationReq policy(String policyTitle) {
        return new CustomerNotificationReq(
                null,
                "POLICY",
                "정책이 변경되었습니다.",
                policyTitle,
                "/policy"
        );
    }
}