package com.green.mmg.admin.penalty.dto;

import com.green.mmg.admin.common.enums.PenaltyLevel;
import com.green.mmg.admin.common.enums.PenaltyTarget;
import lombok.Getter;

@Getter
public class PenaltyReq {
    private PenaltyTarget targetType;
    private Long targetNo;
    private PenaltyLevel level;
    private String reason;
    private Integer durationDays;
}