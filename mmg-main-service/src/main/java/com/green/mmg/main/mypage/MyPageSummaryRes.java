package com.green.mmg.main.mypage;

public record MyPageSummaryRes(
        Long userNo,
        String userId,
        String name,
        String tel,
        Integer green,
        GreenGradeRes greenGrade,
        Long usableCouponCount
) {
}
