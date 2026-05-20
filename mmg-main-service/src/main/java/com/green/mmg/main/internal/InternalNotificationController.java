package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.notification.NotificationService;
import com.green.mmg.main.notification.model.NotificationCreateReq;
import com.green.mmg.main.notification.model.NotificationRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/notification")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResultResponse<NotificationRes> createNotification(
            @RequestBody NotificationCreateReq req
    ) {
        return new ResultResponse<>(
                "알림 생성 성공",
                notificationService.createNotification(req)
        );
    }

}