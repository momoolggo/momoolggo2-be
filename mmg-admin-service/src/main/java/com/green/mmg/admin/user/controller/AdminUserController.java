package com.green.mmg.admin.user;

import com.green.mmg.admin.dto.feign.AdminUserRes;
import com.green.mmg.admin.dto.feign.UserApprovalReq;
import com.green.mmg.admin.dto.feign.UserSuspensionReq;
import com.green.mmg.admin.feign.AuthFeignClient;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthFeignClient authFeignClient;

    /** 전체 회원 목록 조회 */
    @GetMapping
    public ResultResponse<Page<AdminUserRes>> getUserList(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page
    ) {
        return authFeignClient.getUserList(role, page);
    }

    /** 승인 대기 회원 목록 */
    @GetMapping("/pending")
    public ResultResponse<List<AdminUserRes>> getPendingUsers() {
        return authFeignClient.getPendingUsers();
    }

    /** 승인/반려 처리 */
    @PatchMapping("/{userNo}/approval")
    public ResultResponse<Void> updateApproval(
            @PathVariable Long userNo,
            @RequestBody UserApprovalReq req
    ) {
        return authFeignClient.updateApproval(userNo, req);
    }

    /** 계정 정지 */
    @PatchMapping("/{userNo}/suspension")
    public ResultResponse<Void> suspendUser(
            @PathVariable Long userNo,
            @RequestBody UserSuspensionReq req
    ) {
        return authFeignClient.suspendUser(userNo, req);
    }

    /** 계정 정지 해제 */
    @PatchMapping("/{userNo}/suspension/release")
    public ResultResponse<Void> releaseSuspension(@PathVariable Long userNo) {
        return authFeignClient.releaseSuspension(userNo);
    }
}