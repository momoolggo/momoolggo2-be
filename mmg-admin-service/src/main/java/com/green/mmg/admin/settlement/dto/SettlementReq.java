package com.green.mmg.admin.settlement.dto;

import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.common.enums.SettlementTargetType;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class SettlementReq {
    private SettlementTargetType targetType;
    private LocalDate startDate;
    private LocalDate endDate;
    private SettlementsStatus status;
    private Long targetNo;
    private int page = 0;
    private int size = 10;
}