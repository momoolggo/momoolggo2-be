package com.green.mmg.main.feign.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportReviewReq {
    private Long reviewId;
    private Long reporterNo;
    private String reason;
    private String content;
}
