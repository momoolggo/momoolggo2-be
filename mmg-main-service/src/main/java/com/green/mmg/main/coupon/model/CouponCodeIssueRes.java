package com.green.mmg.main.coupon.model;

import java.time.LocalDateTime;

public record CouponCodeIssueRes(
        Long couponListId,
        Long couponId,
        String couponName,
        LocalDateTime expiresAt
) {
}