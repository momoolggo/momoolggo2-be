package com.green.mmg.auth.feign;

import com.green.mmg.auth.feign.dto.OwnerProfileCreateReq;
import com.green.mmg.common.dto.ResultResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "main-owner-profile", url = "${feign.main-service.url}")
public interface MainOwnerProfileClient {

    @PostMapping("/internal/owner-profile")
    ResultResponse<Void> createOwnerProfile(@RequestBody OwnerProfileCreateReq req);
}