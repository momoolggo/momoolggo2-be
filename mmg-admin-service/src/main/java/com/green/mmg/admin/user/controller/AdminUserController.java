package com.green.mmg.admin.user.controller;

import com.green.mmg.admin.dto.feign.*;
import com.green.mmg.admin.delivery.RiderApprovalService;
import com.green.mmg.admin.feign.AuthFeignClient;
import com.green.mmg.admin.feign.MainFeignClient;
import com.green.mmg.admin.feign.RiderFeignClient;
import com.green.mmg.common.dto.ResultResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthFeignClient authFeignClient;
    private final MainFeignClient mainFeignClient;
    private final RiderFeignClient riderFeignClient;
    private final RiderApprovalService riderApprovalService;

    // 전체 회원 목록 조회 (검색 조건 포함)
    @GetMapping
    public ResultResponse<Page<AdminUserRes>> getUserList(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return authFeignClient.getUserList(role, page, userId, name, startDate, endDate);
    }

    // 승인 대기 회원 목록
    @GetMapping("/pending")
    public ResultResponse<List<AdminUserRes>> getPendingUsers() {
        return authFeignClient.getPendingUsers();
    }

    // 승인/반려 처리 — SSE 자동화 트랙(2026-05-21) 정정: auth.user.status 단독 처리.
    // 라이더 가입 시 rider.status는 auto-approve true로 즉시 ACTIVE 박제 (D11 정정, ADR-001 D 폐기).
    @PatchMapping("/{userNo}/approval")
    public ResultResponse<Void> updateApproval(
            @PathVariable Long userNo,
            @RequestBody UserApprovalReq req
    ) {
        return authFeignClient.updateApproval(userNo, req);
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

    //사장 회원가입 프로필
    @GetMapping("/{userNo}/owner-profile")
    public ResultResponse<OwnerProfileRes> getOwnerProfile(@PathVariable Long userNo) {
        return mainFeignClient.getOwnerProfile(userNo);
    }

    /**
     * 라이더 회원가입 프로필 (면허증 사진 등 승인 모달용).
     *
     * <p>rider-service 단건 endpoint 사용 — rider 행 부재(가입 흐름 부분 실패) 시 NOT_FOUND 명시.
     * 이전: getRiderList(null) 전체 받아 인메모리 filter + null 케이스를 "조회 성공"으로 가림 → 가입 미완료 사용자 진단 불가.</p>
     */
    @GetMapping("/{userNo}/rider-profile")
    public ResultResponse<RiderProfileRes> getRiderProfile(@PathVariable Long userNo) {
        try {
            RiderProfileRes profile = riderFeignClient.getRiderProfileByUserNo(userNo);
            return new ResultResponse<>("조회 성공", profile);
        } catch (FeignException.NotFound e) {
            log.warn("라이더 프로필 미등록 userNo={} — 가입 흐름 부분 실패 의심 (auth.user는 존재, rider.rider 행 부재)", userNo);
            return new ResultResponse<>("라이더 프로필이 등록되지 않은 가입 미완료 사용자입니다.", null);
        } catch (Exception e) {
            log.error("라이더 프로필 조회 실패 userNo={}: {}", userNo, e.getMessage(), e);
            return new ResultResponse<>("라이더 프로필 조회 중 오류가 발생했습니다.", null);
        }
    }
}