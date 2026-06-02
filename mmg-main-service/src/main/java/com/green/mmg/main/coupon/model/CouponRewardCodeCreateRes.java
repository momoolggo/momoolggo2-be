package com.green.mmg.main.coupon.model;

import java.time.LocalDateTime;

public record CouponRewardCodeCreateRes(
        String code,
        Integer stage,
        Long couponId,
        String couponName,
        LocalDateTime expiresAt
) {
}