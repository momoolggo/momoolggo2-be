package com.green.mmg.main.notification;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.notification.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;

    @Transactional(readOnly = true)
    public List<NotificationRes> getNotifications(Long userNo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return notificationRepository.findByUserNoOrderByCreatedAtDesc(userNo, pageable).stream()
                .map(NotificationRes::from)
                .toList();
    }

    @Transactional
    public NotificationReadRes readNotification(Long userNo, Long notificationId) {
        Notification notification = notificationRepository.findByNotificationIdAndUserNo(notificationId, userNo)
                .orElseThrow(() -> new BusinessException("알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        notification.markAsRead();

        return new NotificationReadRes(true);
    }

    @Transactional
    public NotificationReadAllRes readAllNotifications(Long userNo) {
        List<Notification> notifications = notificationRepository.findByUserNoAndReadFalse(userNo);

        for (Notification notification : notifications) {
            notification.markAsRead();
        }

        return new NotificationReadAllRes(notifications.size());
    }

    @Transactional
    public NotificationRes createNotification(NotificationCreateReq req) {
        Notification notification = Notification.create(
                req.userNo(),
                req.notificationType(),
                req.title(),
                req.content(),
                req.targetUrl()
        );

        Notification savedNotification = notificationRepository.saveAndFlush(notification);

        notificationSseService.sendNotification(
                savedNotification.getUserNo(),
                NotificationEventRes.from(savedNotification)
        );

        return NotificationRes.from(savedNotification);
    }

}