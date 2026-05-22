package com.green.mmg.main.greenpoint;

import com.green.mmg.main.feign.AuthFeignClient;
import com.green.mmg.main.feign.model.GreenPointAddReq;
import com.green.mmg.main.order.model.Orders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GreenPointRewardService {
    private static final int ECO_GREEN_POINT = 1;
    private static final String ECO_REASON = "일회용품 미사용";
    private static final int MAX_RETRY_COUNT = 5;

    private final GreenPointLogRepository greenPointLogRepository;
    private final AuthFeignClient authFeignClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rewardIfEcoSelected(Orders order) {
        if (!Boolean.TRUE.equals(order.getEcoSelected())) {
            return;
        }

        if (greenPointLogRepository.existsByOrderId(order.getOrderId())) {
            return;
        }

        GreenPointLog log = greenPointLogRepository.saveAndFlush(
                GreenPointLog.pending(order.getOrderId(), order.getUserNo(), ECO_GREEN_POINT, ECO_REASON)
        );

        requestGreenPoint(log);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void retryFailedRewards() {
        List<GreenPointLog> logs = greenPointLogRepository
                .findByStatusInAndRetryCountLessThanOrderByGreenPointLogIdAsc(
                        List.of("PENDING", "FAILED"),
                        MAX_RETRY_COUNT,
                        PageRequest.of(0, 20)
                );

        for (GreenPointLog log : logs) {
            requestGreenPoint(log);
        }
    }

    private void requestGreenPoint(GreenPointLog greenPointLog) {
        try {
            authFeignClient.addGreenPoint(
                    greenPointLog.getUserNo(),
                    new GreenPointAddReq(greenPointLog.getPoint(), greenPointLog.getReason(), greenPointLog.getOrderId()));
            greenPointLog.markSuccess();
        } catch (Exception e) {
            greenPointLog.markFailed(e.getMessage());
            log.warn("그린포인트 적립 재시도 대상 orderId={} userNo={} retryCount={} error={}",
                    greenPointLog.getOrderId(), greenPointLog.getUserNo(), greenPointLog.getRetryCount(), e.getMessage());
        }
    }
}