package com.green.mmg.main.owner.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OwnerSettlementRes {
    private Long settlementId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer grossAmount;
    private Integer feeAmount;
    private Integer netAmount;
    private Integer itemCount;
    private String status;
    private LocalDateTime paidAt;
}