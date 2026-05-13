package com.green.mmg.common.feign;

import com.green.mmg.common.dto.feign.UserBriefDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * main/rider/admin → auth-service Internal API 호출용.
 * Spring Cloud OpenFeign이 classpath에 있는 서비스에서만 활성화.
 */
@FeignClient(name = "auth-service", url = "${AUTH_SERVICE_URL:http://localhost:8081}")
@ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignClient")
public interface AuthFeignClient {

    /** 단건 조회 — 가게 상세에 사장 이름 등 1명에 대한 fetch 시 */
    @GetMapping("/internal/auth/user/{userNo}")
    UserBriefDto getUser(@PathVariable("userNo") long userNo);

    /** Batch 조회 — 주문 목록 등 N개 user_no에 대한 fetch (N+1 회피) */
    @GetMapping("/internal/auth/users")
    List<UserBriefDto> getUsers(@RequestParam("ids") List<Long> userNos);

    /** 사장 정보 — 현재는 UserBriefDto와 동일. Phase 5에서 OwnerInfoDto로 확장 가능 */
    @GetMapping("/internal/auth/owner/{userNo}")
    UserBriefDto getOwner(@PathVariable("userNo") long userNo);
}
