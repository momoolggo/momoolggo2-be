package com.green.mmg.admin.settlement.dto;

import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.common.enums.SettlementTargetType;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class SettlementReq {
    private SettlementTargetType targetType;
    private LocalDate startDate;
    private LocalDate endDate;
    private SettlementsStatus status;
    private Long targetNo;
}