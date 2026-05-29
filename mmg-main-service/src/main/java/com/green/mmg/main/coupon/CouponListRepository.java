package com.green.mmg.main.coupon;

import com.green.mmg.main.coupon.model.CouponList;
import com.green.mmg.main.coupon.model.CouponListRes;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponListRepository extends JpaRepository<CouponList, Long> {

    @Query("""
            SELECT new com.green.mmg.main.coupon.model.CouponListRes(
                cl.couponListId,
                c.couponId,
                c.name,
                c.discountValue,
                cl.expiresAt
            )
            FROM CouponList cl, Coupon c
            WHERE cl.couponId = c.couponId
              AND cl.userNo = :userNo
              AND COALESCE(cl.used, false) = false
              AND cl.expiresAt >= :now
              AND c.isActive = true
            ORDER BY cl.expiresAt ASC, cl.couponListId DESC
            """)
    List<CouponListRes> findUsableCouponsByUserNo(
            @Param("userNo") Long userNo,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT COUNT(cl)
            FROM CouponList cl, Coupon c
            WHERE cl.couponId = c.couponId
              AND cl.userNo = :userNo
              AND COALESCE(cl.used, false) = false
              AND cl.expiresAt >= :now
              AND c.isActive = true
            """)
    long countUsableCouponsByUserNo(
            @Param("userNo") Long userNo,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT cl
            FROM CouponList cl, Coupon c
            WHERE cl.couponId = c.couponId
              AND cl.userNo = :userNo
              AND cl.couponId = :couponId
              AND COALESCE(cl.used, false) = false
              AND cl.orderId IS NULL
              AND cl.expiresAt >= :now
              AND c.isActive = true
            ORDER BY cl.expiresAt ASC, cl.couponListId DESC
            LIMIT 1
            """)
    Optional<CouponList> findFirstUsableCoupon(
            @Param("userNo") Long userNo,
            @Param("couponId") Long couponId,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT cl
            FROM CouponList cl, Coupon c
            WHERE cl.couponId = c.couponId
              AND cl.couponListId = :couponListId
              AND cl.userNo = :userNo
              AND COALESCE(cl.used, false) = false
              AND cl.orderId IS NULL
              AND cl.expiresAt >= :now
              AND c.isActive = true
            """)
    Optional<CouponList> findUsableCouponByCouponListId(
            @Param("userNo") Long userNo,
            @Param("couponListId") Long couponListId,
            @Param("now") LocalDateTime now
    );

    Optional<CouponList> findFirstByOrderIdAndUserNoAndUsedFalse(Long orderId, Long userNo);

    List<CouponList> findAllByOrderIdAndUsedFalse(Long orderId);

    boolean existsByUserNoAndCouponId(Long userNo, Long couponId);
}