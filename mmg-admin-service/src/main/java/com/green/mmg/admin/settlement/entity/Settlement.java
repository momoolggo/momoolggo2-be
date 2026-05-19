package com.green.mmg.admin.settlement.entity;

import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.common.enums.SettlementTargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "settlement")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long settlementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private SettlementTargetType targetType;

    @Column(name = "target_no", nullable = false)
    private Long targetNo;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "gross_amount")
    private Integer grossAmount;

    @Column(name = "fee_amount")
    private Integer feeAmount;

    @Column(name = "tax_amount")
    private Integer taxAmount;

    @Column(name = "other_deduction")
    private Integer otherDeduction;

    @Column(name = "net_amount")
    private Integer netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private SettlementsStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 토스페이먼츠 지급대행 ID (지급 요청 후 저장)
    @Column(name = "toss_payout_id", length = 100)
    private String tossPayoutId;

    // 정산 상태 변경 (pending → done)
    public void complete() {
        this.status = SettlementsStatus.DONE;
        this.paidAt = LocalDateTime.now();
    }

    // 정산 보류
    public void hold() {
        this.status = SettlementsStatus.HELD;
    }

    // 토스 지급 ID 저장
    public void setTossPayoutId(String payoutId) {
        this.tossPayoutId = payoutId;
    }
}