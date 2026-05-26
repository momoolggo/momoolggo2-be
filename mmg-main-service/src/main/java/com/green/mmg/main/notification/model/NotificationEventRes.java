package com.green.mmg.main.notification.model;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationEventRes(
        Long notificationId,
        String type,
        String title,
        String content,
        boolean read,
        LocalDateTime createdAt,
        String targetUrl,
        Map<String, Object> payload
) {
    public static NotificationEventRes from(Notification notification) {
        return new NotificationEventRes(
                notification.getNotificationId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getContent(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getTargetUrl(),
                Map.of()
        );
    }
}