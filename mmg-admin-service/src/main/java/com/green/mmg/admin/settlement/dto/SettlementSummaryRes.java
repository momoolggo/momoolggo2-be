package com.green.mmg.admin.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettlementSummaryRes {
    private Long expectedAmount;        // 이번 주 PENDING 예정액
    private Long monthlyExpectedAmount; // 이번 달 전체(모든 상태) 예정액
    private Long completedAmount;       // 완료 정산 금액
    private Long completedCount;        // 완료 정산 건수
    private Long pendingCount;          // 대기 중 건수 (가게)
    private Long pendingStoreCount;     // 가게 대기 건수
    private Long pendingRiderCount;     // 라이더 대기 건수 (rider-service 별도 관리, 현재 0)
}