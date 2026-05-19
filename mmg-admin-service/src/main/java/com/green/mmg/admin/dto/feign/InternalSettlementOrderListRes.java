package com.green.mmg.admin.dto.feign;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class InternalSettlementOrderListRes {
    private Long storeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalSales;
    private List<InternalSettlementOrderRes> orders;
}
