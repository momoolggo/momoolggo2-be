package com.green.mmg.admin.dto.feign;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class InternalSettlementOrderRes {
    private Long orderId;
    private String menuName;
    private Long orderAmount;
    private LocalDateTime orderTime;
}
