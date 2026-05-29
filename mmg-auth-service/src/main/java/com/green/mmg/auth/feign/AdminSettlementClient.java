package com.green.mmg.auth.feign;

import com.green.mmg.common.dto.ResultResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "mmg-admin-settlement",
        url = "${feign.admin-service.url:http://localhost:8083}"
)
public interface AdminSettlementClient {

    @GetMapping("/internal/settlement/stores/unpaid/exists")
    ResultResponse<Boolean> hasUnpaidStoreSettlement(@RequestParam("storeIds") List<Long> storeIds);
}
