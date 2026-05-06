package com.green.mmg.admin.blind.dto;

import com.green.mmg.admin.common.enums.BlindReason;
import com.green.mmg.admin.common.enums.BlindStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BlindRes {
    private Long blindId;
    private Long reviewNo;
    private Long userNo;
    private BlindReason reason;
    private Integer durationDays;
    private BlindStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
}