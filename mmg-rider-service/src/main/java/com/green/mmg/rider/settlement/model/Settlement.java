package com.green.mmg.rider.settlement.model;

import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * settlement 테이블 엔티티 (my_mmg_rider.settlement).
 *
 * <p>외부 참조 (논리 FK, 물리 FK 제약 X — CLAUDE.md §3 MSA 경계):
 * <ul>
 *   <li>{@code rider_no} → rider.rider_no</li>
 *   <li>{@code confirmed_by_admin_no} → my_mmg_admin.admin (NULLABLE — admin confirm 시 기록)</li>
 * </ul>
 *
 * <p>BaseEntity 상속: created_at / updated_at 컬럼 자동 매핑 (R1-A Rider / R2-a Delivery 패턴 일관).
 * UPDATE 다수 도메인 (admin confirm 시 status/confirmed_at/confirmed_by_admin_no, 입금 시 paid_at).</p>
 *
 * <p>setter 미공개 — confirm/markPaid 등 명시 메서드는 R7 SettlementService 진입 시 추가.
 * R2 시점은 entity 형태 + 주간 집계 INSERT 시점 생성자만 도입 (status PENDING 고정).</p>
 *
 * <p>관련 ADR: ADR-002 line 155-181 (정정 5) + ADR-007 (Phase 5-R7 본격 구현).
 * 인덱스 1건: rider_no (Q-R2a2 (나) 자동 적용).</p>
 */
@Entity
@Table(name = "settlement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_no")
    private Long settlementNo;

    @Column(name = "rider_no", nullable = false)
    private Long riderNo;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "delivery_count", nullable = false)
    private Integer deliveryCount;

    @Column(name = "total_distance_m", nullable = false)
    private Integer totalDistanceM;

    @Column(name = "total_base_fee", nullable = false)
    private Integer totalBaseFee;

    @Column(name = "total_extra_fee", nullable = false)
    private Integer totalExtraFee;

    @Column(name = "commission", nullable = false)
    private Integer commission;

    @Column(name = "tax", nullable = false)
    private Integer tax;

    @Column(name = "insurance", nullable = false)
    private Integer insurance;

    @Column(name = "payout", nullable = false)
    private Integer payout;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SettlementStatus status;

    @Column(name = "confirmed_by_admin_no")
    private Long confirmedByAdminNo;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * 주간 집계 INSERT 시점 생성자 — R7 SettlementService.
     * status 고정: PENDING (admin confirm 대기 — D10-b). confirmed/paid 필드 null.
     */
    public Settlement(Long riderNo, LocalDate periodStart, LocalDate periodEnd,
                      Integer deliveryCount, Integer totalDistanceM,
                      Integer totalBaseFee, Integer totalExtraFee,
                      Integer commission, Integer tax, Integer insurance, Integer payout) {
        this.riderNo = riderNo;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.deliveryCount = deliveryCount;
        this.totalDistanceM = totalDistanceM;
        this.totalBaseFee = totalBaseFee;
        this.totalExtraFee = totalExtraFee;
        this.commission = commission;
        this.tax = tax;
        this.insurance = insurance;
        this.payout = payout;
        this.status = SettlementStatus.PENDING;
    }

    // 비즈니스 메서드 (confirm / markPaid) — R7 SettlementService 진입 시 추가.
}
