package com.green.mmg.admin.notification.service;

import com.green.mmg.admin.dto.feign.CustomerNotificationReq;
import com.green.mmg.admin.feign.MainFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerNotificationService {

    private final MainFeignClient mainFeignClient;

    public void sendNoticeCreated(String noticeTitle) {
        sendAfterCommit(CustomerNotificationReq.notice(noticeTitle));
    }

    public void sendPolicyChanged(String policyTitle) {
        sendAfterCommit(CustomerNotificationReq.policy(policyTitle));
    }

    private void sendAfterCommit(CustomerNotificationReq req) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send(req);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send(req);
            }
        });
    }

    private void send(CustomerNotificationReq req) {
        try {
            mainFeignClient.createCustomerNotification(req);
        } catch (Exception e) {
            log.warn("고객 알림 생성 요청 실패. type={}, title={}",
                    req.notificationType(),
                    req.title(),
                    e
            );
        }
    }
}