package com.green.mmg.rider.settlement.dto;

import com.green.mmg.rider.settlement.model.Settlement;
import com.green.mmg.rider.settlement.model.SettlementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 라이더 정산 내역 row — GET /api/rider/settlement. ADR-007 line 130-145 응답 dto 박제 일관.
 *
 * <p>Group 5.5 정정 (2026-05-17, code-reviewer W-2): {@code riderNo} 필드 신설 (15 필드).
 * admin 모니터 화면 (GET /api/admin/rider-settlement/pending)에서 어느 라이더의 정산인지 식별 가능하도록.
 * 후방 호환 — 기존 라이더 본인 정산 조회 응답에 자기 식별자 노출 무해.</p>
 */
public record SettlementRowRes(
        Long settlementNo,
        Long riderNo,
        LocalDate periodStart,
        LocalDate periodEnd,
        Integer deliveryCount,
        Integer totalDistanceM,
        Integer totalBaseFee,
        Integer totalExtraFee,
        Integer commission,
        Integer tax,
        Integer insurance,
        Integer payout,
        SettlementStatus status,
        LocalDateTime confirmedAt,
        LocalDateTime paidAt
) {
    public static SettlementRowRes from(Settlement s) {
        return new SettlementRowRes(
                s.getSettlementNo(),
                s.getRiderNo(),
                s.getPeriodStart(),
                s.getPeriodEnd(),
                s.getDeliveryCount(),
                s.getTotalDistanceM(),
                s.getTotalBaseFee(),
                s.getTotalExtraFee(),
                s.getCommission(),
                s.getTax(),
                s.getInsurance(),
                s.getPayout(),
                s.getStatus(),
                s.getConfirmedAt(),
                s.getPaidAt()
        );
    }
}
