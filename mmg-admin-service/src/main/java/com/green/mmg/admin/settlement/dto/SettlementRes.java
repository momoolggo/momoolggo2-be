package com.green.mmg.admin.settlement.dto;

import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.common.enums.SettlementTargetType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SettlementRes {
    private Long settlementId;
    private SettlementTargetType targetType;
    private Long targetNo;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer itemCount;
    private Integer grossAmount;
    private Integer feeAmount;
    private Integer taxAmount;
    private Integer otherDeduction;
    private Integer netAmount;
    private SettlementsStatus status;
    private LocalDateTime paidAt;
    private String bankAccount;
    private LocalDateTime createdAt;
}