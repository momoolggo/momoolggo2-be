package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.internal.dto.InternalSettlementOrderListRes;
import com.green.mmg.main.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/settlement")
public class InternalSettlementController {
    private final OrderService orderService;

    @GetMapping("/orders")
    public ResultResponse<InternalSettlementOrderListRes> getSettlementOrders(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return new ResultResponse<>("정산 주문 내역 조회 완료",
                orderService.getSettlementOrders(storeId, startDate, endDate));
    }
}
