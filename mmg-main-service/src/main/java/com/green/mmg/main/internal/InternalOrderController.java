package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.internal.dto.DeliveryCompleteReq;
import com.green.mmg.main.internal.dto.DeliveryCompleteRes;
import com.green.mmg.main.internal.dto.DeliveryStatusUpdateReq;
import com.green.mmg.main.internal.dto.DeliveryStatusUpdateRes;
import com.green.mmg.main.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/order")
public class InternalOrderController {

    private final OrderService orderService;

    /**
     * 배달 상태 변경 알림 — interfaces.md §2.1. rider → main.
     * ADR-004 매핑(line 90-98) — OrderService.updateDeliveryStatus가 7값 String → 1/2/3 int 매핑.
     * X-Internal 검증 X (Gateway InternalBlockController 차단 일관, Phase 6+ mTLS 강화 박제).
     */
    @PutMapping("/{orderId}/delivery-status")
    public DeliveryStatusUpdateRes updateDeliveryStatus(
            @PathVariable Long orderId,
            @RequestBody DeliveryStatusUpdateReq req
    ) {
        return orderService.updateDeliveryStatus(orderId, req.deliveryStatus());
    }

    /**
     * 배달 완료 처리 — interfaces.md §2.2. rider → main.
     * delivery_state=3 (DELIVERED 종결, ADR-004) + order_state=6 (CLAUDE.md §7 종결, Q-A8.e-2 (가)).
     * completedAt body 인자 수신 후 무시 (Q-A8.e-1 (나), orders.completed_at 컬럼 부재 — tech-debt).
     */
    @PostMapping("/{orderId}/complete")
    public DeliveryCompleteRes completeDelivery(
            @PathVariable Long orderId,
            @RequestBody DeliveryCompleteReq req
    ) {
        return orderService.completeDelivery(orderId, req.completedAt());
    }

    /** 그린포인트 적립/POST /internal/order/{orderId}/greenpoint */
    @PostMapping("/{orderId}/greenpoint")
    public ResultResponse<Map<String, Integer>> addGreenPoint(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body
    ) {
        // TODO: 일회용품 미사용 주문 완료 시 그린포인트 적립

        return new ResultResponse<>("적립 완료", Map.of("totalPoint", 130));
    }
}