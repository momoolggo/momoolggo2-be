package com.green.mmg.main.owner.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OwnerWriterRatingStatsRes {
    private Long userNo;
    private Double avgRating;
    private Long reviewCount;
}
