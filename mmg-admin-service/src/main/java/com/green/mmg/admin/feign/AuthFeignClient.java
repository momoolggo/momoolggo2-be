package com.green.mmg.admin.feign;

import com.green.mmg.common.dto.feign.UserBriefDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mmg-auth-service",
        url = "${feign.auth-service.url:http://localhost:8081}")
public interface AuthFeignClient {

    // 회원 단건 조회
    @GetMapping("/internal/auth/user/{userNo}")
    UserBriefDto getUser(@PathVariable("userNo") long userNo);

    // 회원 배치 조회
    @GetMapping("/internal/auth/users")
    List<UserBriefDto> getUsers(@RequestParam("ids") List<Long> userNos);

    // 사장님 단건 조회
    @GetMapping("/internal/auth/owner/{userNo}")
    UserBriefDto getOwner(@PathVariable("userNo") long userNo);
}