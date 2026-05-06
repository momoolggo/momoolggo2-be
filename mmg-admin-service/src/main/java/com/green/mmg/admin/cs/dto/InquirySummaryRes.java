package com.green.mmg.admin.cs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InquirySummaryRes {
    private Long totalInquiryCount;    // 총 문의 건수
    private Long autoResolvedCount;    // 자동 응대 완료 건수
    private Long pendingInquiryCount;  // 미처리 문의 건수
}