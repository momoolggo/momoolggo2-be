package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.feign.AuthFeignClient;
import com.green.mmg.main.internal.dto.InternalDailyStatsRes;
import com.green.mmg.main.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/stats")
public class InternalStatsController {

    private final OrderRepository orderRepository;
    private final AuthFeignClient authFeignClient;

    @Transactional(readOnly = true)
    @GetMapping("/daily")
    public ResultResponse<InternalDailyStatsRes> getDailyStats() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long totalOrders = orderRepository.countTodayOrders(start, end);
        long totalRevenue = orderRepository.sumTodayRevenue(start, end);
        long newUsers = authFeignClient.getTodayNewUsers().getResultData();

        return new ResultResponse<>("일별 통계 조회 완료",
                new InternalDailyStatsRes(totalOrders, newUsers, totalRevenue));
    }
}