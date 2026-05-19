package com.green.mmg.admin.dto.feign;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InternalReviewRes {
    private Long reviewId;
    private String storeName;
    private Long userNo;
    private String writer;
    private String content;
    private Double rating;
}
