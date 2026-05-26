package com.green.mmg.main.notification.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "target_url")
    private String targetUrl;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Notification create(
            Long userNo,
            String notificationType,
            String title,
            String content,
            String targetUrl
    ) {
        Notification notification = new Notification();
        notification.userNo = userNo;
        notification.notificationType = notificationType;
        notification.title = title;
        notification.content = content;
        notification.targetUrl = targetUrl;
        notification.read = false;
        notification.createdAt = LocalDateTime.now();
        return notification;
    }

    public void markAsRead() {
        if (read) {
            return;
        }

        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}