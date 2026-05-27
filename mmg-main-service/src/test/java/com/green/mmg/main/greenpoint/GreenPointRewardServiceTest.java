package com.green.mmg.main.greenpoint;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.feign.AuthFeignClient;
import com.green.mmg.main.feign.model.GreenPointAddReq;
import com.green.mmg.main.order.model.Orders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GreenPointRewardServiceTest {
    private static final Long USER_NO = 42L;
    private static final Long ORDER_ID = 901L;

    @Mock private GreenPointLogRepository greenPointLogRepository;
    @Mock private AuthFeignClient authFeignClient;
    @Mock private EcoLevelCouponRewardService ecoLevelCouponRewardService;
    @InjectMocks private GreenPointRewardService greenPointRewardService;

    @Test
    @DisplayName("친환경 선택 주문 적립 성공 후 등급 보상 검사를 수행한다")
    void rewardIfEcoSelectedIssuesLevelRewardAfterGreenPointSuccess() {
        Orders order = new Orders();
        order.setOrderId(ORDER_ID);
        order.setUserNo(USER_NO);
        order.setEcoSelected(true);
        when(greenPointLogRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(greenPointLogRepository.saveAndFlush(any(GreenPointLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(authFeignClient.addGreenPoint(eq(USER_NO), any(GreenPointAddReq.class)))
                .thenReturn(new ResultResponse<>("그린포인트 적립 완료", 10));

        greenPointRewardService.rewardIfEcoSelected(order);

        verify(ecoLevelCouponRewardService).issueRewardsForCrossedStages(USER_NO, 9, 10);
    }

    @Test
    @DisplayName("이미 적립된 주문은 그린포인트와 보상 쿠폰을 재지급하지 않는다")
    void alreadyRewardedOrderSkipped() {
        Orders order = new Orders();
        order.setOrderId(ORDER_ID);
        order.setUserNo(USER_NO);
        order.setEcoSelected(true);
        when(greenPointLogRepository.existsByOrderId(ORDER_ID)).thenReturn(true);

        greenPointRewardService.rewardIfEcoSelected(order);

        verifyNoInteractions(authFeignClient, ecoLevelCouponRewardService);
    }
}
