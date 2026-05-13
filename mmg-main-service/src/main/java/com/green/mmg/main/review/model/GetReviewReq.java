package com.green.mmg.main.review.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetReviewReq {
    private long userNo;
    private int currentPage = 1;
    private int size = 5;

    public int getStartIdx() {
        return (currentPage - 1) * size;
    }
}
