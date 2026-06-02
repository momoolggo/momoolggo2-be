package com.green.mmg.main.greenpoint;

import java.util.Arrays;
import java.util.List;

public enum EcoLevelRewardStage {
    SPROUT(10, "친환경 새싹 1,000원 할인 쿠폰"),
    LEVEL_3(15, "친환경 나무 3,000원 할인 쿠폰"),
    LEVEL_4(40, "친환경 숲 5,000원 할인 쿠폰"),
    LEVEL_5(62, "친환경 지구 7,000원 할인 쿠폰");

    private final int minPoint;
    private final String couponName;

    EcoLevelRewardStage(int minPoint, String couponName) {
        this.minPoint = minPoint;
        this.couponName = couponName;
    }

    public String getCouponName() {
        return couponName;
    }

    public boolean crossedBy(int beforePoint, int afterPoint) {
        return beforePoint < minPoint && afterPoint >= minPoint;
    }

    public static List<EcoLevelRewardStage> crossedStages(int beforePoint, int afterPoint) {
        return Arrays.stream(values())
                .filter(stage -> stage.crossedBy(beforePoint, afterPoint))
                .toList();
    }
}
