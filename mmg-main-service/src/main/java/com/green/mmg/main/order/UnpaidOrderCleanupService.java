package com.green.mmg.main.order;

import com.green.mmg.main.coupon.CouponListRepository;
import com.green.mmg.main.coupon.model.CouponList;
import com.green.mmg.main.order.model.Orders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnpaidOrderCleanupService {

    private static final int PAY_STATE_UNPAID = 1;
    private static final int CLEANUP_DELAY_MINUTES = 30;
    private static final int CLEANUP_BATCH_SIZE = 50;

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CouponListRepository couponListRepository;
    private final OrderMapper orderMapper;

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void cleanupStaleUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(CLEANUP_DELAY_MINUTES);
        List<Orders> orders = orderRepository.findByPayStateAndOrderTimeBeforeOrderByOrderTimeAsc(
                PAY_STATE_UNPAID,
                cutoff,
                PageRequest.of(0, CLEANUP_BATCH_SIZE)
        );

        for (Orders order : orders) {
            cleanupOrder(order);
        }
    }

    private void cleanupOrder(Orders order) {
        Long orderId = order.getOrderId();
        Long storeId = order.getStoreId();

        couponListRepository.findAllByOrderIdAndUsedFalse(orderId)
                .forEach(CouponList::releaseReservation);

        orderDetailRepository.deleteByOrderId(orderId);
        int deleted = orderRepository.deleteByOrderIdAndPayStateUnpaid(orderId);

        if (deleted > 0 && storeId != null) {
            orderMapper.calSumOrder(storeId);
            log.info("결제 미완료 임시 주문 정리 orderId={} storeId={}", orderId, storeId);
        }
    }
}
