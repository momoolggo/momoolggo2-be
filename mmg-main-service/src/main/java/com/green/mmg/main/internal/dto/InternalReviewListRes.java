package com.green.mmg.main.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InternalReviewListRes {
    private Long reviewId;
    private String storeName;

    @JsonIgnore
    private Long userNo;

    private String writer;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
}
