package com.green.mmg.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardRes {
    // Feign 완성 후 추가
    // private Long totalUsers;
    // private Long totalStores;
    // private Long totalReviews;


    private Long totalBlinds;       // 블라인드 건수
    private Long pendingInquiries;  // 미처리 문의 수
    private Long pendingSettlements; // 정산 대기 건수
}