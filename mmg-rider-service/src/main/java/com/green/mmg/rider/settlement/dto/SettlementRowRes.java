package com.green.mmg.rider.settlement.dto;

import com.green.mmg.rider.settlement.model.Settlement;
import com.green.mmg.rider.settlement.model.SettlementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 라이더 정산 내역 row — GET /api/rider/settlement. ADR-007 line 130-145 응답 dto 박제 일관.
 */
public record SettlementRowRes(
        Long settlementNo,
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
