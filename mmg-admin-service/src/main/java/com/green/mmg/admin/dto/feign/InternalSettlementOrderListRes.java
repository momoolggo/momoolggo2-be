package com.green.mmg.admin.dto.feign;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class InternalSettlementOrderListRes {
    private Long storeId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Long totalSales;
    private List<InternalSettlementOrderRes> orders;
}
