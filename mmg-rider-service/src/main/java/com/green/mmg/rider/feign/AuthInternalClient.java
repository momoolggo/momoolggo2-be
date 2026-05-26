package com.green.mmg.rider.feign;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.dto.feign.UserBriefDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * rider → auth-service Internal API 호출용.
 * 현재 사용처: RiderService.joinProfile — 가입 시 auth user.tel 자동 조회.
 */
@FeignClient(name = "auth-service-from-rider", url = "${AUTH_SERVICE_URL:http://localhost:8081}")
@ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignClient")
public interface AuthInternalClient {

    @GetMapping("/internal/auth/user/{userNo}")
    ResultResponse<UserBriefDto> getUser(@PathVariable("userNo") long userNo);
}
