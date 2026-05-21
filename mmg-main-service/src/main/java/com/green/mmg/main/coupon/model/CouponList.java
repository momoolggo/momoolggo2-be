package com.green.mmg.main.coupon.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "couponlist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "couponlist_id")
    private Long couponListId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "is_used")
    private Boolean used;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "issued_at", insertable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public void reserveForOrder(Long orderId){
        this.orderId = orderId;
    }

    public void markUsed(){
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }


}