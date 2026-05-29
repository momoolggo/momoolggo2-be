package com.green.mmg.main.coupon.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CouponListRes(
        Long couponListId,
        Long couponId,
        String name,
        String discountType,
        Integer discount,
        LocalDate expiry
) {
    public CouponListRes(Long couponListId, Long couponId, String name, String discountType, Integer discount, LocalDateTime expiresAt) {
        this(
                couponListId,
                couponId,
                name,
                discountType,
                discount,
                expiresAt == null ? null : expiresAt.toLocalDate()
        );
    }
}
