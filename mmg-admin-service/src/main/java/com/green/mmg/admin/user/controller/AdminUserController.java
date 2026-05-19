package com.green.mmg.admin.user.controller;

import com.green.mmg.admin.dto.feign.AdminUserRes;
import com.green.mmg.admin.dto.feign.UserAddressRes;
import com.green.mmg.admin.dto.feign.UserApprovalReq;
import com.green.mmg.admin.dto.feign.UserSuspensionReq;
import com.green.mmg.admin.delivery.RiderApprovalService;
import com.green.mmg.admin.feign.AuthFeignClient;
import com.green.mmg.admin.feign.MainFeignClient;  // 추가
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthFeignClient authFeignClient;
    private final MainFeignClient mainFeignClient;  // 추가
    private final RiderApprovalService riderApprovalService;  // ADR-001 (D) 라이더 통합 승인

  //전체 회원 목록 조회
    @GetMapping
    public ResultResponse<Page<AdminUserRes>> getUserList(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page
    ) {
        return authFeignClient.getUserList(role, page);
    }

    // 승인 대기 회원 목록
    @GetMapping("/pending")
    public ResultResponse<List<AdminUserRes>> getPendingUsers() {
        return authFeignClient.getPendingUsers();
    }

    // 승인/반려 처리 — ADR-001 (D) 라이더/일반 회원 통합 승인 (2026-05-19 정정).
    // admin 1회 클릭(/admin/user 화면)으로 auth.user.status 변경 + 라이더면 rider.status도 동시 변경.
    @PatchMapping("/{userNo}/approval")
    public ResultResponse<Void> updateApproval(
            @PathVariable Long userNo,
            @RequestBody UserApprovalReq req
    ) {
        ResultResponse<Void> authRes = authFeignClient.updateApproval(userNo, req);

        // status=ACTIVE 승인 시점에만 라이더 통합 처리. REJECTED/PENDING은 auth 단독.
        if ("ACTIVE".equals(req.getStatus())) {
            riderApprovalService.approveByUserNoIfRider(userNo);
        }
        return authRes;
    }

    // 계정 정지
    @PatchMapping("/{userNo}/suspension")
    public ResultResponse<Void> suspendUser(
            @PathVariable Long userNo,
            @RequestBody UserSuspensionReq req
    ) {
        return authFeignClient.suspendUser(userNo, req);
    }

    //계정 정지 해제
    @PatchMapping("/{userNo}/suspension/release")
    public ResultResponse<Void> releaseSuspension(@PathVariable Long userNo) {
        return authFeignClient.releaseSuspension(userNo);
    }

    //사장 가게 주소 조회
    @GetMapping("/{userNo}/store-location")
    public ResponseEntity<String> getStoreLocation(@PathVariable Long userNo) {
        String location = mainFeignClient.getOwnerStoreLocation(userNo).getResultData();
        return ResponseEntity.ok(location);
    }

    //회원 기본 주소 조회
    @GetMapping("/{userNo}/address")
    public ResponseEntity<String> getMemberAddress(@PathVariable Long userNo) {
        ResultResponse<List<UserAddressRes>> res = mainFeignClient.getUserAddresses(userNo);
        List<UserAddressRes> data = res.getResultData();
        if (data == null || data.isEmpty()) return ResponseEntity.ok(null);
        String address = data.stream()
                .filter(a -> a.getDefaultAd() != null && a.getDefaultAd() == 1)
                .map(a -> a.getAddress() + " " +
                        (a.getAddressDetail() != null ? a.getAddressDetail() : ""))
                .findFirst()
                .orElse(null);
        return ResponseEntity.ok(address);
    }

    // 고객상세정보
    @GetMapping("/{userNo}/detail")
    public ResultResponse<?> getUserDetail(@PathVariable Long userNo) {
        return authFeignClient.getUserDetail(userNo);
    }

    /**
     * 회원 삭제 — ADR-001 (D) cascade 보완 (2026-05-19 신설).
     * 순서: rider 먼저 삭제(없으면 skip) → auth 삭제. rider 실패 시 throw, auth 미실행 (안전).
     * MSA 박제: 두 schema 별이라 DB 자동 cascade X — application 레벨 보장 필수.
     */
    @DeleteMapping("/{userNo}")
    public ResultResponse<Void> deleteUser(@PathVariable Long userNo) {
        riderApprovalService.deleteByUserNoIfRider(userNo);
        return authFeignClient.deleteUser(userNo);
    }
}