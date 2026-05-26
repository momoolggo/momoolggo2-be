package com.green.mmg.admin.dto.feign;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class InternalSettlementOrderRes {
    private Long orderId;
    private String menuName;
    private Long orderAmount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime orderTime;
}
