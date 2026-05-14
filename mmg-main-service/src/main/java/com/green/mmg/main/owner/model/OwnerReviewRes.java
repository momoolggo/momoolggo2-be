package com.green.mmg.main.owner.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class OwnerReviewRes {
    private Long reviewId;

    @JsonIgnore
    private Long userNo;

    private String userName;
    private String menuName;
    private Integer rating;
    private String contents;
    private String photo;
    private LocalDateTime writtenAt;
    private LocalDateTime amendedAt;
}
