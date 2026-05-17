package com.green.mmg.admin.delivery;

import com.green.mmg.admin.dto.feign.RiderApproveReq;
import com.green.mmg.admin.dto.feign.RiderSuspendReq;
import com.green.mmg.admin.feign.RiderFeignClient;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * admin 측 라이더 관리 화면 endpoint — Group 8.5 신설 (2026-05-17, Q-A1 (라+) + Q-A14.b (3)).
 *
 * <p>Q-A19 (다) 분리 패턴: admin 외부 PATCH (BlindController 패턴 일관, status 정정 의미) + rider Internal Feign POST (interfaces.md §3.1/§3.2 박제).
 * Q-A18 (b) cross-schema 정합성: admin → auth user.status 정지 (AdminUserController.suspendUser, 별 영역) vs rider §3.2 SUSPENDED — *별 호출*. Phase 6+ outbox tech-debt 등재.
 * Q-A20 (가) Rider entity 전이 검증: rider Provider 측에서 PENDING→ACTIVE / ?→SUSPENDED 검증 throw.</p>
 *
 * <p>try-catch X — Feign 예외 propagate (admin 측 5xx, 사용자 재시도 가능). AdminDeliveryController 패턴 일관.</p>
 */
@RestController
@RequestMapping("/api/admin/rider")
@RequiredArgsConstructor
public class RiderApprovalController {

    private final RiderFeignClient riderFeignClient;

    /** 라이더 승인 — PENDING → ACTIVE. BlindController PATCH 패턴 일관. */
    @PatchMapping("/{riderNo}/approve")
    public ResultResponse<?> approve(@PathVariable Long riderNo,
                                     @RequestBody RiderApproveReq req) {
        riderFeignClient.approveRider(riderNo, req);
        return new ResultResponse<>("라이더 승인 완료", null);
    }

    /** 라이더 제재 — ?→SUSPENDED. reason은 audit log Phase 6+ outbox 위임 (Q-A18 (b)). */
    @PatchMapping("/{riderNo}/suspend")
    public ResultResponse<?> suspend(@PathVariable Long riderNo,
                                     @RequestBody RiderSuspendReq req) {
        riderFeignClient.suspendRider(riderNo, req);
        return new ResultResponse<>("라이더 제재 완료", null);
    }
}
