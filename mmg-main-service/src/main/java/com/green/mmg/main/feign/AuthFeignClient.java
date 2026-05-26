package com.green.mmg.main.feign;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.main.feign.model.GreenPointAddReq;
import com.green.mmg.main.feign.model.InternalUserDetailRes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "mmg-auth-service",
        url = "${feign.auth-service.url:http://localhost:8081}"
)
public interface AuthFeignClient {

    @GetMapping("/internal/auth/user/{userNo}")
    ResultResponse<UserBriefDto> getUserInfo(@PathVariable("userNo") Long userNo);

    @GetMapping("/internal/auth/users")
    ResultResponse<List<UserBriefDto>> getUsers(@RequestParam("ids") List<Long> userNos);

    @GetMapping("/internal/auth/owner/{userNo}")
    ResultResponse<UserBriefDto> getOwnerInfo(@PathVariable("userNo") Long userNo);

    @GetMapping("/internal/auth/owners/search")
    ResultResponse<List<Long>> searchOwnerUserNos(@RequestParam(required = false) String userId,
                                                  @RequestParam(required = false) String name);

    @GetMapping("/internal/auth/rider/user-nos")
    ResultResponse<List<Long>> getRiderUserNos();

    @GetMapping("/internal/auth/rider/count")
    ResultResponse<Long> getRiderCount();

    @GetMapping("/internal/auth/user/{userNo}/detail")
    ResultResponse<InternalUserDetailRes> getUserDetail(@PathVariable("userNo") Long userNo);

    @GetMapping("/internal/auth/stats/new-users")
    ResultResponse<Long> getTodayNewUsers();

    @GetMapping("/internal/auth/stats/new-users/range")
    ResultResponse<Long> getNewUsersByRange(@RequestParam("start") String start,
                                            @RequestParam("end") String end);

    @PostMapping("/internal/auth/user/{userNo}/greenpoint")
    ResultResponse<Integer> addGreenPoint(@PathVariable("userNo") Long userNo,
                                          @RequestBody GreenPointAddReq req);
}