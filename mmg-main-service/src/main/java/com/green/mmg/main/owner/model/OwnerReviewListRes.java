package com.green.mmg.main.owner.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class OwnerReviewListRes {
    private Double avgRating;
    private Map<Integer, Long> ratingStats;
    private List<OwnerReviewRes> reviews;
}
