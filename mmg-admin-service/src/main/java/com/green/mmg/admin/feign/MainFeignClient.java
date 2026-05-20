package com.green.mmg.admin.feign;

import com.green.mmg.admin.dto.feign.*;
import com.green.mmg.common.dto.ResultResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.green.mmg.admin.dto.feign.InternalStoreListPageRes;

import java.util.List;

@FeignClient(name = "mmg-main-service", url = "${feign.main-service.url}")
public interface MainFeignClient {

    @GetMapping("/internal/user/{userNo}/address")
    ResultResponse<List<UserAddressRes>> getUserAddresses(@PathVariable("userNo") Long userNo);

    @GetMapping("/internal/stats/today")
    ResultResponse<TodayStatsRes> getTodayStats();

    @GetMapping("/internal/user/owner/{ownerNo}/store-location")
    ResultResponse<String> getOwnerStoreLocation(@PathVariable("ownerNo") Long ownerNo);

    @GetMapping("/internal/review/list")
    ResultResponse<List<InternalReviewListRes>> getReviewList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    );

    @GetMapping("/internal/store/list")
    ResultResponse<InternalStoreListPageRes> getStoreList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String date
    );

    @GetMapping("/internal/stats/chart")
    ResultResponse<List<InternalChartStatsRes>> getChartStats(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "storeCount") String metric
    );

    @GetMapping("/internal/store/{storeId}")
    ResultResponse<?> getStoreDetail(@PathVariable("storeId") Long storeId);

    @GetMapping("/internal/review/{reviewId}")
    ResultResponse<InternalReviewRes> getReviewById(@PathVariable("reviewId") Long reviewId);

    @GetMapping("/internal/settlement/orders")
    ResultResponse<InternalSettlementOrderListRes> getSettlementOrders(
            @RequestParam("storeId") Long storeId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    );

    @PostMapping("/internal/notification/customers")
    void createCustomerNotification(@RequestBody CustomerNotificationReq req);

    @PostMapping("/internal/notification")
    void createNotification(@RequestBody CustomerNotificationReq req);

}