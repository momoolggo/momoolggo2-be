package com.green.mmg.rider.delivery.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/rider/order/completed 응답 본체 — R9 배달내역.
 *
 * <p>합계 필드 (REQ-RDR-003 "누적 배달건/이동거리/배달비 합계" 박제 일관, Q-DTO (나)).
 * totalDistance는 R7 정산 진입 시점에 산출 (현재는 0 placeholder).</p>
 */
public record DeliveryHistoryRes(
        LocalDate from,
        LocalDate to,
        Integer totalCount,
        Integer totalFee,
        List<DeliveryHistoryRowRes> rows
) {
}
