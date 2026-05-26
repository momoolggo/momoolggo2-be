package com.green.mmg.main.internal.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InternalReviewListRes {
    private Long reviewId;
    private String storeName;
    private Long userNo;
    private String writer;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
}
