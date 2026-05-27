package com.green.mmg.main.greenpoint;

import com.green.mmg.main.coupon.CouponListRepository;
import com.green.mmg.main.coupon.CouponRepository;
import com.green.mmg.main.coupon.model.Coupon;
import com.green.mmg.main.coupon.model.CouponList;
import com.green.mmg.main.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EcoLevelCouponRewardServiceTest {
    private static final Long USER_NO = 42L;

    @Mock private CouponRepository couponRepository;
    @Mock private CouponListRepository couponListRepository;
    @Mock private NotificationService notificationService;
    @InjectMocks private EcoLevelCouponRewardService service;

    @Test
    @DisplayName("새싹 기준 도달 시 1,000원 보상 쿠폰을 1회 지급한다")
    void sproutRewardIssuedOnce() {
        Coupon coupon = coupon(1L);
        when(couponRepository.findFirstByNameAndIssueTypeAndIsActiveTrue(
                "친환경 새싹 1,000원 할인 쿠폰",
                "ECO_LEVEL"
        ))
                .thenReturn(Optional.of(coupon));
        when(couponListRepository.existsByUserNoAndCouponId(USER_NO, 1L)).thenReturn(false);
        when(couponListRepository.save(any(CouponList.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int issuedCount = service.issueRewardsForCrossedStages(USER_NO, 9, 10);

        assertThat(issuedCount).isEqualTo(1);
        ArgumentCaptor<CouponList> couponListCaptor = ArgumentCaptor.forClass(CouponList.class);
        verify(couponListRepository).save(couponListCaptor.capture());
        assertThat(couponListCaptor.getValue().getUserNo()).isEqualTo(USER_NO);
        assertThat(couponListCaptor.getValue().getCouponId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 받은 단계 보상은 중복 지급하지 않는다")
    void alreadyRewardedStageSkipped() {
        Coupon coupon = coupon(1L);
        when(couponRepository.findFirstByNameAndIssueTypeAndIsActiveTrue(
                "친환경 새싹 1,000원 할인 쿠폰",
                "ECO_LEVEL"
        ))
                .thenReturn(Optional.of(coupon));
        when(couponListRepository.existsByUserNoAndCouponId(USER_NO, 1L)).thenReturn(true);

        int issuedCount = service.issueRewardsForCrossedStages(USER_NO, 9, 10);

        assertThat(issuedCount).isZero();
        verify(couponListRepository, never()).save(any());
    }

    @Test
    @DisplayName("여러 단계를 한 번에 넘으면 지나친 단계 보상을 모두 지급한다")
    void skippedStagesAllIssued() {
        Coupon level3Coupon = coupon(3L);
        Coupon level4Coupon = coupon(4L);
        when(couponRepository.findFirstByNameAndIssueTypeAndIsActiveTrue(
                "친환경 나무 3,000원 할인 쿠폰",
                "ECO_LEVEL"
        ))
                .thenReturn(Optional.of(level3Coupon));
        when(couponRepository.findFirstByNameAndIssueTypeAndIsActiveTrue(
                "친환경 숲 5,000원 할인 쿠폰",
                "ECO_LEVEL"
        ))
                .thenReturn(Optional.of(level4Coupon));
        when(couponListRepository.existsByUserNoAndCouponId(USER_NO, 3L)).thenReturn(false);
        when(couponListRepository.existsByUserNoAndCouponId(USER_NO, 4L)).thenReturn(false);
        when(couponListRepository.save(any(CouponList.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int issuedCount = service.issueRewardsForCrossedStages(USER_NO, 14, 40);

        assertThat(issuedCount).isEqualTo(2);
        verify(couponListRepository, times(2)).save(any(CouponList.class));
    }

    @Test
    @DisplayName("같은 단계 안에서 점수만 오르면 보상 쿠폰을 재지급하지 않는다")
    void sameStageNoReward() {
        int issuedCount = service.issueRewardsForCrossedStages(USER_NO, 11, 14);

        assertThat(issuedCount).isZero();
        verifyNoInteractions(couponRepository, couponListRepository);
    }

    @Test
    @DisplayName("5단계 기준 도달 시 7,000원 보상 쿠폰을 지급한다")
    void level5RewardIssued() {
        Coupon coupon = coupon(5L);
        when(couponRepository.findFirstByNameAndIssueTypeAndIsActiveTrue(
                "친환경 지구 7,000원 할인 쿠폰",
                "ECO_LEVEL"
        ))
                .thenReturn(Optional.of(coupon));
        when(couponListRepository.existsByUserNoAndCouponId(USER_NO, 5L)).thenReturn(false);
        when(couponListRepository.save(any(CouponList.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int issuedCount = service.issueRewardsForCrossedStages(USER_NO, 61, 62);

        assertThat(issuedCount).isEqualTo(1);
        verify(couponListRepository).save(any(CouponList.class));
    }

    private Coupon coupon(Long couponId) {
        Coupon coupon = mock(Coupon.class);
        when(coupon.getCouponId()).thenReturn(couponId);
        return coupon;
    }
}
