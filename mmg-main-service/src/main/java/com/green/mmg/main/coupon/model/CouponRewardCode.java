package com.green.mmg.main.coupon.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_reward_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponRewardCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_code_id")
    private Long rewardCodeId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "event_code", nullable = false, length = 50)
    private String eventCode;

    @Column(name = "reward_stage", nullable = false)
    private Integer rewardStage;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used")
    private Boolean used;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "couponlist_id")
    private Long couponListId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static CouponRewardCode create(Long userNo, String eventCode, Integer rewardStage,
                                          Long couponId, String code, LocalDate issueDate,
                                          LocalDateTime expiresAt) {
        CouponRewardCode rewardCode = new CouponRewardCode();
        rewardCode.userNo = userNo;
        rewardCode.eventCode = eventCode;
        rewardCode.rewardStage = rewardStage;
        rewardCode.couponId = couponId;
        rewardCode.code = code;
        rewardCode.issueDate = issueDate;
        rewardCode.expiresAt = expiresAt;
        rewardCode.used = false;
        return rewardCode;
    }

    public void markUsed(Long couponListId) {
        this.used = true;
        this.usedAt = LocalDateTime.now();
        this.couponListId = couponListId;
    }
}