package com.green.mmg.main.internal;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/order")
public class InternalOrderController {

    /** 배달 상태 변경 알림/ PUT /internal/order/{orderId}/delivery-status */
    @PutMapping("/{orderId}/delivery-status")
    public ResultResponse<Void> updateDeliveryStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body
    ) {
        // TODO: 라이더가 보낸 배달 상태를 주문 상태에 반영

        return new ResultResponse<>("상태 변경 완료", null);
    }

    /** 배달 완료 처리/ POST /internal/order/{orderId}/complete */
    @PostMapping("/{orderId}/complete")
    public ResultResponse<Void> completeDelivery(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body
    ) {
        // TODO: 배달 완료 처리 + 주문 상태 최종 변경 + 고객 알림 트리거

        return new ResultResponse<>("배달 완료 처리", null);
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