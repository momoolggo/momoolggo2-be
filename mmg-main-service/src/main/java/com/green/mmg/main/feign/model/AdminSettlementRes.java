package com.green.mmg.main.feign.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AdminSettlementRes {
    private Long settlementId;
    private String targetType;
    private Long targetNo;
    private String targetName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer itemCount;
    private Integer grossAmount;
    private Integer feeAmount;
    private Integer taxAmount;
    private Integer otherDeduction;
    private Integer netAmount;
    private String status;
    private LocalDateTime paidAt;
    private String bankAccount;
    private LocalDateTime createdAt;
}
