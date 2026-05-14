package com.green.mmg.admin.feign;

import com.green.mmg.admin.dto.feign.TodayStatsRes;
import com.green.mmg.admin.dto.feign.UserAddressRes;
import com.green.mmg.common.dto.ResultResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "mmg-main-service", url = "${feign.main-service.url}")
public interface MainFeignClient {

    @GetMapping("/internal/user/{userNo}/address")
    ResultResponse<List<UserAddressRes>> getUserAddresses(@PathVariable("userNo") Long userNo);

    @GetMapping("/internal/stats/today")
    ResultResponse<TodayStatsRes> getTodayStats();

}