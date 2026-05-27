package com.green.mmg.main.greenpoint;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.coupon.CouponListRepository;
import com.green.mmg.main.coupon.CouponRepository;
import com.green.mmg.main.coupon.model.Coupon;
import com.green.mmg.main.coupon.model.CouponList;
import com.green.mmg.main.notification.NotificationService;
import com.green.mmg.main.notification.model.NotificationCreateReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcoLevelCouponRewardService {
    private static final String ECO_LEVEL_ISSUE_TYPE = "ECO_LEVEL";

    private final CouponRepository couponRepository;
    private final CouponListRepository couponListRepository;
    private final NotificationService notificationService;

    @Transactional
    public int issueRewardsForCrossedStages(Long userNo, int beforePoint, int afterPoint) {
        int issuedCount = 0;

        for (EcoLevelRewardStage stage : EcoLevelRewardStage.crossedStages(beforePoint, afterPoint)) {
            Coupon coupon = couponRepository.findFirstByNameAndIssueTypeAndIsActiveTrue(
                            stage.getCouponName(),
                            ECO_LEVEL_ISSUE_TYPE
                    )
                    .orElseThrow(() -> new BusinessException(
                            "친환경 등급 보상 쿠폰을 찾을 수 없습니다: " + stage.getCouponName(),
                            HttpStatus.NOT_FOUND
                    ));

            if (couponListRepository.existsByUserNoAndCouponId(userNo, coupon.getCouponId())) {
                continue;
            }

            couponListRepository.save(CouponList.issue(userNo, coupon, LocalDateTime.now()));
            sendCouponIssuedNotification(userNo, coupon);
            issuedCount++;
        }

        return issuedCount;
    }

    private void sendCouponIssuedNotification(Long userNo, Coupon coupon) {
        notificationService.createNotification(new NotificationCreateReq(
                userNo,
                "COUPON_ISSUED",
                "쿠폰이 발급되었습니다",
                coupon.getName() + "이 발급되었습니다. 쿠폰함에서 확인해 주세요.",
                "/mypage/coupon"
        ));
    }
}