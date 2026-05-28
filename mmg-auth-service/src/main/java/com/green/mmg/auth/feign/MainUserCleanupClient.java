package com.green.mmg.auth.feign;

import com.green.mmg.auth.feign.dto.OwnerWithdrawCheckRes;
import com.green.mmg.common.dto.ResultResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "mmg-main-user-cleanup",
        url = "${feign.main-service.url:http://localhost:8080}"
)
public interface MainUserCleanupClient {

    @GetMapping("/internal/user/{userNo}/active-orders/exists")
    ResultResponse<Boolean> hasActiveOrders(@PathVariable Long userNo);

    @GetMapping("/internal/user/{ownerNo}/owner-withdraw-check")
    ResultResponse<OwnerWithdrawCheckRes> checkOwnerWithdraw(@PathVariable Long ownerNo);

    @PostMapping("/internal/user/{userNo}/withdraw-cleanup")
    ResultResponse<Void> cleanupWithdrawnUser(@PathVariable Long userNo);
}
