package com.green.mmg.admin.penalty.dto;

import com.green.mmg.admin.common.enums.PenaltyLevel;
import com.green.mmg.admin.common.enums.PenaltyTarget;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class PenaltyRes {
    private Long penaltyId;
    private PenaltyTarget targetType;
    private Long targetNo;
    private PenaltyLevel level;
    private String reason;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
}