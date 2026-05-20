package com.green.mmg.main.notification;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.notification.model.NotificationReadAllRes;
import com.green.mmg.main.notification.model.NotificationReadRes;
import com.green.mmg.main.notification.model.NotificationRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationSseService notificationSseService;
    private final NotificationService notificationService;

    @GetMapping
    public ResultResponse<List<NotificationRes>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return new ResultResponse<>(
                "조회 성공",
                notificationService.getNotifications(principal.getSignedUserNo(), page, size)
        );
    }

    @PutMapping("/{notificationId}/read")
    public ResultResponse<NotificationReadRes> readNotification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long notificationId
    ) {
        return new ResultResponse<>(
                "읽음 처리 완료",
                notificationService.readNotification(principal.getSignedUserNo(), notificationId)
        );
    }

    @PutMapping("/read-all")
    public ResultResponse<NotificationReadAllRes> readAllNotifications(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return new ResultResponse<>(
                "전체 읽음 처리 완료",
                notificationService.readAllNotifications(principal.getSignedUserNo())
        );
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserPrincipal principal) {
        return notificationSseService.subscribe(principal.getSignedUserNo());
    }

}