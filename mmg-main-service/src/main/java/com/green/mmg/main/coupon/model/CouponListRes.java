package com.green.mmg.main.coupon.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CouponListRes(
        Long couponId,
        String name,
        Integer discount,
        LocalDate expiry
) {
    public CouponListRes(Long couponId, String name, Integer discount, LocalDateTime expiresAt) {
        this(
                couponId,
                name,
                discount,
                expiresAt == null ? null : expiresAt.toLocalDate()
        );
    }
}