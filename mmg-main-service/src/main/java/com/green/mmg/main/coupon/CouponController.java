package com.green.mmg.main.coupon;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.coupon.model.CouponCodeIssueReq;
import com.green.mmg.main.coupon.model.CouponCodeIssueRes;
import com.green.mmg.main.coupon.model.CouponListRes;
import com.green.mmg.main.coupon.model.CouponRewardCodeCreateReq;
import com.green.mmg.main.coupon.model.CouponRewardCodeCreateRes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponRewardCodeService couponRewardCodeService;

    @GetMapping
    public ResultResponse<List<CouponListRes>> getMyCoupons(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return new ResultResponse<>(
                "조회 성공",
                couponService.getMyCoupons(principal.getSignedUserNo())
        );
    }

    @PostMapping("/event/food-cup/code")
    public ResultResponse<CouponRewardCodeCreateRes> createFoodCupRewardCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CouponRewardCodeCreateReq req
    ) {
        return new ResultResponse<>(
                "쿠폰 코드 발급 완료",
                couponRewardCodeService.createFoodCupRewardCode(principal.getSignedUserNo(), req.stage())
        );
    }

    @PostMapping("/code")
    public ResultResponse<CouponCodeIssueRes> issueCouponByCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CouponCodeIssueReq req
    ) {
        return new ResultResponse<>(
                "쿠폰 등록 완료",
                couponRewardCodeService.issueCouponByCode(principal.getSignedUserNo(), req.code())
        );
    }
}