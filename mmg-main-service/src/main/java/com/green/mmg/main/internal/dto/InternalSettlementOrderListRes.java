package com.green.mmg.main.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class InternalSettlementOrderListRes {
    private Long storeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalSales;
    private List<InternalSettlementOrderRes> orders;
}