package com.green.mmg.admin.delivery;

import com.green.mmg.admin.dto.feign.RiderSettlementCalculateReq;
import com.green.mmg.admin.dto.feign.RiderSettlementConfirmReq;
import com.green.mmg.admin.dto.feign.RiderSettlementRowRes;
import com.green.mmg.admin.feign.RiderFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * admin 측 라이더 정산 화면 endpoint — Group 5 신설 (2026-05-17, team-handoff §9 부채 해소).
 *
 * <p>Q-A10.a (옵션 1) 일관: admin Settlement 도메인은 라이더 외 정산 (점주/플랫폼, target_type enum)이고
 * 본 controller는 *라이더 정산 source of truth* (rider DB) Feign 호출만 처리. admin Settlement과 결 분리.
 * Q-A10.b (iii) 별 controller 신설 / Q-A10.c (a) 기존 RiderFeignClient 재사용.</p>
 *
 * <p>try-catch X — Feign 예외 그대로 propagate (admin 측 5xx, 사용자 재시도 가능). D1-bis와 다른 정책 —
 * admin은 동기 호출자라 실패 즉시 노출 의도 (Q-A10.b 결정 결과). AdminDeliveryController 패턴 일관 (Controller 직접 Feign).</p>
 */
@RestController
@RequestMapping("/api/admin/rider-settlement")
@RequiredArgsConstructor
public class RiderSettlementController {

    private final RiderFeignClient riderFeignClient;

    /** 주간 정산 집계 트리거 (D10-b 멱등). rider Provider /internal/rider/settlement/calculate. */
    @PostMapping("/calculate")
    public List<RiderSettlementRowRes> calculate(@RequestBody RiderSettlementCalculateReq req) {
        return riderFeignClient.calculateRiderSettlement(req);
    }

    /** PENDING → CONFIRMED. */
    @PostMapping("/{settlementNo}/confirm")
    public RiderSettlementRowRes confirm(@PathVariable Long settlementNo,
                                         @RequestBody RiderSettlementConfirmReq req) {
        return riderFeignClient.confirmRiderSettlement(settlementNo, req);
    }

    /** PENDING 목록 — admin 모니터. */
    @GetMapping("/pending")
    public List<RiderSettlementRowRes> pending() {
        return riderFeignClient.getRiderSettlementPending();
    }
}
