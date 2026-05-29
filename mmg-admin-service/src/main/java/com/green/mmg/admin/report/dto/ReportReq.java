package com.green.mmg.admin.report.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportReq {
    private Long reviewId;
    private Long reporterNo;
    private String reason;
    private String content;
    private String reviewContent; // AI 판정용 리뷰 본문 스냅샷
}
