package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.feign.AuthFeignClient;
import com.green.mmg.main.internal.dto.InternalChartStatsRes;
import com.green.mmg.main.internal.dto.InternalDailyStatsRes;
import com.green.mmg.main.internal.dto.InternalTodayStatsRes;
import com.green.mmg.main.order.OrderRepository;
import com.green.mmg.main.review.ReviewRepository;
import com.green.mmg.main.store.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/stats")
public class InternalStatsController {

    private final OrderRepository orderRepository;
    private final AuthFeignClient authFeignClient;
    private final StoreMapper storeMapper;
    private final ReviewRepository reviewRepository;

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

    @Transactional(readOnly = true)
    @GetMapping("/today")
    public ResultResponse<InternalTodayStatsRes> getTodayStats(){
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long newUserCount = authFeignClient.getTodayNewUsers().getResultData();
        long storeCount = storeMapper.countTodayStores(start, end);
        long reviewCount = reviewRepository.countTodayReviews(start, end);

        return new ResultResponse<>("조회 성공",
                new InternalTodayStatsRes(newUserCount, storeCount, reviewCount)
                );
    }

    @Transactional(readOnly = true)
    @GetMapping("/chart")
    public ResultResponse<List<InternalChartStatsRes>> getChartStats(@RequestParam(defaultValue = "weekly") String period,
                                                                     @RequestParam(defaultValue = "storeCount") String metric){
        LocalDate today = LocalDate.now();
        List<InternalChartStatsRes> result = new ArrayList<>();

    if("weekly".equals(period)) {
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            result.add(new InternalChartStatsRes(formatDateLabel(day), countByMetric(metric, day.atStartOfDay(), day.plusDays(1).atStartOfDay())));

        }
    } else if("monthly".equals(period)) {
        for (int day = 1; day <= today.getDayOfMonth(); day++) {
            LocalDate date = LocalDate.of(today.getYear(), today.getMonth(), day);
            result.add(new InternalChartStatsRes(formatDateLabel(date), countByMetric(metric,date.atStartOfDay(), date.plusDays(1).atStartOfDay())));
        }
    } else if("yearly".equals(period)) {
        for (int month = 1; month <= 12; month ++) {
            LocalDate start = LocalDate.of(today.getYear(), month, 1);
            LocalDate end = start.plusMonths(1);
            result.add(new InternalChartStatsRes(month + "월",
                                                countByMetric(metric, start.atStartOfDay(), end.atStartOfDay())));
        }
    } else {
        throw new BusinessException("period는 weekly, monthly, yearly만 가능합니다", HttpStatus.BAD_REQUEST);
    }
        return new ResultResponse<>("차트 통계 조회 완료", result);
    }

    private long countByMetric(String metric, LocalDateTime start, LocalDateTime end) {
        return switch (metric) {
            case "memberCount" -> {
                Long count = authFeignClient.getNewUsersByRange(start.toLocalDate().toString(), end.toLocalDate().toString())
                        .getResultData();
                yield count == null ? 0L : count;
            }
            case "storeCount" -> storeMapper.countStoresByCreatedAtBetween(start, end);
            case "reviewCount" -> reviewRepository.countReviewsByCreatedAtBetween(start, end);
            default -> throw new BusinessException("metric은 memberCount, storeCount, reviewCount만 가능합니다", HttpStatus.BAD_REQUEST);
        };
    }
    private String formatDateLabel(LocalDate date) {
        String[] days = {"월", "화", "수", "목", "금", "토", "일"};
        return date.getMonthValue() + "/" + date.getDayOfMonth() + "("
                + days[date.getDayOfWeek().getValue() - 1] + ")";
    }
}