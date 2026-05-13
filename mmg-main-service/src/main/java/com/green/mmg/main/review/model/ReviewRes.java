package com.green.mmg.main.review.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRes {
    private long reviewId;
    private String storeName;
    private String menuName;   // 대표 메뉴 1개
    private int rating;
    private String contents;
    private String photo;
    private String date;       // 포맷된 날짜
}
