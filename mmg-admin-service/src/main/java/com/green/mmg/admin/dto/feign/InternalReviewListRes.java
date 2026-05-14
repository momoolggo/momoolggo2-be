package com.green.mmg.admin.dto.feign;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class InternalReviewListRes {
    private Long reviewId;
    private String storeName;
    private String writer;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
}