package com.green.mmg.main.internal.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InternalSettlementOrderRes {
    private Long orderId;
    private String menuName;
    private Long orderAmount;
    private LocalDateTime orderTime;
}
