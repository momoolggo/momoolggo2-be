package com.green.mmg.main.coupon;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.coupon.model.CouponListRes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ResultResponse<List<CouponListRes>> getMyCoupons(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return new ResultResponse<>(
                "조회 성공",
                couponService.getMyCoupons(principal.getSignedUserNo())
        );
    }
}