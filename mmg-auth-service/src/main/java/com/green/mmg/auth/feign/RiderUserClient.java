package com.green.mmg.auth.feign;

import com.green.mmg.common.dto.ResultResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "mmg-rider-user",
        url = "${feign.rider-service.url:http://localhost:8082}"
)
public interface RiderUserClient {

    @GetMapping("/internal/rider/users/{userNo}/active-work/exists")
    ResultResponse<Boolean> hasActiveWork(@PathVariable Long userNo);
}