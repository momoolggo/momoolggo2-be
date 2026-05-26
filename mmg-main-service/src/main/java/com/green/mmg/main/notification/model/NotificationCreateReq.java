package com.green.mmg.main.notification.model;

public record NotificationCreateReq(
        Long userNo,
        String notificationType,
        String title,
        String content,
        String targetUrl
) {
}