package com.green.mmg.admin.dto.feign;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * admin ← rider Feign: 라이더 정산 row 응답 (interfaces.md §3.3/§3.4 + rider Provider {@code SettlementRowRes} 일관, Group 5 신설 2026-05-17).
 *
 * <p>rider 측 {@code com.green.mmg.rider.settlement.dto.SettlementRowRes} record 15 필드 1:1 일관 (case-#34 영역 분리 강제).
 * Group 5.5 정정 (code-reviewer W-2): {@code riderNo} 필드 추가 — admin 모니터 화면에서 어느 라이더 정산인지 식별 필수.
 * {@code status}: enum {@code SettlementStatus} 그대로 매핑되도록 String으로 박제 — admin은 본 도메인 별 enum 없음 (Jackson "PENDING"/"CONFIRMED" 문자열 그대로).</p>
 */
public record RiderSettlementRowRes(
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
        String status,
        LocalDateTime confirmedAt,
        LocalDateTime paidAt
) {
}
