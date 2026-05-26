package com.green.mmg.main.coupon;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.coupon.model.Coupon;
import com.green.mmg.main.coupon.model.CouponList;
import com.green.mmg.main.coupon.model.CouponListRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponListRepository couponListRepository;
    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public List<CouponListRes> getMyCoupons(Long userNo) {
        return couponListRepository.findUsableCouponsByUserNo(userNo, LocalDateTime.now());
    }

    @Transactional
    public int applyCouponToOrder(Long userNo, Long orderId, Long couponId, int orderAmount){
        if (couponId == null) {
            return orderAmount;
        }

        CouponList couponList = couponListRepository
                .findFirstUsableCoupon(userNo, couponId, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("사용 가능한 쿠폰이 아닙니다", HttpStatus.BAD_REQUEST));

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException("쿠폰 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        int discountAmount = calculateDiscountAmount(coupon, orderAmount);
        int finalAmount = Math.max(orderAmount - discountAmount, 0);

        couponList.reserveForOrder(orderId);

        return finalAmount;
    }

    @Transactional
    public void markCouponUsedByOrder(Long userNo, Long orderId){
        couponListRepository.findFirstByOrderIdAndUserNoAndUsedFalse(orderId, userNo)
                .ifPresent(CouponList::markUsed);
    }

    private int calculateDiscountAmount(Coupon coupon, int orderAmount){
        Integer minAmount = coupon.getMinDiscountAmount();
        if (minAmount != null && orderAmount < minAmount){
            throw new BusinessException("쿠폰 최소 주문금액을 충족하지 못합니다", HttpStatus.BAD_REQUEST);
        }

        Integer discountValue = coupon.getDiscountValue();
        if (discountValue == null || discountValue <=0 ){
            throw new BusinessException("쿠폰 할인값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        String discountType = coupon.getDiscountType();
        int discountAmount;

        if ("PERCENT".equalsIgnoreCase(discountType)) {
            discountAmount = orderAmount * discountValue / 100;
        } else if ("FIXED".equalsIgnoreCase(discountType)){
            discountAmount = discountValue;
        } else {
            throw new BusinessException("지원하지 않는 쿠폰 유형입니다.", HttpStatus.BAD_REQUEST);
        }

        Integer maxAmount = coupon.getMaxDiscountAmount();
        if (maxAmount != null && maxAmount > 0){
            discountAmount = Math.min(discountAmount, maxAmount);
        }
        return  Math.min(discountAmount, orderAmount);
    }

}