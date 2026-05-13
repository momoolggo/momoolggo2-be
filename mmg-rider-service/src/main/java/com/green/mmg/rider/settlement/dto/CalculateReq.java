package com.green.mmg.rider.settlement.dto;

import java.time.LocalDate;

/**
 * Internal admin → rider — 주간 정산 집계 요청 (POST /internal/rider/settlement/calculate).
 *
 * <p>periodStart=월요일, periodEnd=일요일 (D10-b 박제). admin이 매주 월요일 새벽에 호출.
 * 멱등 처리 — 동일 주 (riderNo + periodStart + periodEnd) 이미 INSERT된 경우 기존 행 반환.</p>
 */
public record CalculateReq(
        LocalDate periodStart,
        LocalDate periodEnd
) {
}
