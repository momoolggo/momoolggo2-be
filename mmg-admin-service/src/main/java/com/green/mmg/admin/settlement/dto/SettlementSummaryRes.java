package com.green.mmg.admin.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettlementSummaryRes {
    private Integer expectedAmount;    // 예상 정산 금액
    private Integer completedAmount;   // 완료 정산 금액
    private Long completedCount;       // 완료 정산 건수
    private Long pendingCount;         // 대기 중 건수 (가게)
    private Long pendingStoreCount;    // 가게 대기 건수
    private Long pendingRiderCount;    // 라이더 대기 건수 (rider-service 별도 관리, 현재 0)
}