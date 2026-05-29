package com.green.mmg.main.coupon.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CouponListRes(
        Long couponListId,
        Long couponId,
        String name,
        Integer discount,
        LocalDate expiry
) {
    public CouponListRes(Long couponListId, Long couponId, String name, Integer discount, LocalDateTime expiresAt) {
        this(
                couponListId,
                couponId,
                name,
                discount,
                expiresAt == null ? null : expiresAt.toLocalDate()
        );
    }
}