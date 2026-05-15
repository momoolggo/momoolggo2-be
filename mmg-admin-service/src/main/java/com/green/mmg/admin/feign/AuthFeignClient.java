package com.green.mmg.admin.feign;

import com.green.mmg.admin.dto.feign.AdminUserRes;
import com.green.mmg.admin.dto.feign.InternalUserDetailRes;
import com.green.mmg.admin.dto.feign.UserApprovalReq;
import com.green.mmg.admin.dto.feign.UserSuspensionReq;
import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.dto.feign.UserBriefDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

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

    // 전체 회원 목록
    @GetMapping("/internal/auth/users/list")
    ResultResponse<Page<AdminUserRes>> getUserList(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page
    );

    // 승인 대기 회원 목록
    @GetMapping("/internal/auth/users/pending")
    ResultResponse<List<AdminUserRes>> getPendingUsers();

    // 승인/반려 처리
    @PatchMapping("/internal/auth/user/{userNo}/approval")
    ResultResponse<Void> updateApproval(
            @PathVariable("userNo") Long userNo,
            @RequestBody UserApprovalReq req
    );

    // 계정 정지
    @PatchMapping("/internal/auth/user/{userNo}/suspension")
    ResultResponse<Void> suspendUser(
            @PathVariable("userNo") Long userNo,
            @RequestBody UserSuspensionReq req
    );

    // 계정 정지 해제
    @PatchMapping("/internal/auth/user/{userNo}/suspension/release")
    ResultResponse<Void> releaseSuspension(@PathVariable("userNo") Long userNo);

    // 라이더 수 조회
    @GetMapping("/internal/auth/rider/count")
    ResultResponse<Long> getRiderCount();

    @GetMapping("/internal/auth/user/{userNo}/detail")
    ResultResponse<InternalUserDetailRes> getUserDetail(@PathVariable("userNo") Long userNo);
}
