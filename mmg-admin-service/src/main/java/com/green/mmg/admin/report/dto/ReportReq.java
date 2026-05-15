package com.green.mmg.admin.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportReq {
    private Long reviewId;
    private Long reporterNo;
    private String reason;
    private String content;
}
