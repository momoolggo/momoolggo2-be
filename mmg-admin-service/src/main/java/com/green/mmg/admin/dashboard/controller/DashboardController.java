package com.green.mmg.admin.dashboard.controller;

import com.green.mmg.admin.dashboard.service.DashboardService;
import com.green.mmg.admin.feign.MainFeignClient;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final MainFeignClient mainFeignClient;

    @GetMapping
    public ResultResponse<?> getDashboard() {
        return new ResultResponse<>("조회 성공", dashboardService.getDashboard());
    }

    @GetMapping("/chart")
    public ResultResponse<?> getChartStats(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "storeCount") String metric) {
        return mainFeignClient.getChartStats(period, metric);
    }
}