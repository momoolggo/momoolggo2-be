package com.green.mmg.main.feign;

import com.green.mmg.main.feign.model.AuthOwnerInfoRes;
import com.green.mmg.main.feign.model.AuthUserInfoRes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "mmg-auth-service",
        url = "${feign.auth-service.url:http://localhost:8081}"
)
public interface AuthFeignClient {

    @GetMapping("/internal/auth/user/{userNo}")
    AuthUserInfoRes getUserInfo(@PathVariable("userNo") Long userNo);

    @GetMapping("/internal/auth/owner/{userNo}")
    AuthOwnerInfoRes getOwnerInfo(@PathVariable("userNo") Long userNo);
}