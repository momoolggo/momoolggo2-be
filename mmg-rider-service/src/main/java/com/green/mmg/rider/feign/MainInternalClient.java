package com.green.mmg.rider.feign;

import com.green.mmg.rider.feign.dto.DeliveryCompleteReq;
import com.green.mmg.rider.feign.dto.DeliveryCompleteRes;
import com.green.mmg.rider.feign.dto.DeliveryStatusUpdateReq;
import com.green.mmg.rider.feign.dto.DeliveryStatusUpdateRes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * rider → main-service Internal API 호출용 (interfaces.md §2.1).
 *
 * <p>Phase 4-A {@code AuthFeignClient} 정착 패턴 일관:
 * {@code @FeignClient(name, url) + @ConditionalOnClass}. Spring Cloud OpenFeign이 classpath에 있는
 * 서비스(mmg-rider-service)에서만 활성화 — 단위 테스트 (mock 의존)에서는 무시.</p>
 *
 * <p>호출 시점: DeliveryService 메서드 트랜잭션 외부 (ADR-004 line 144-148 박제 일관).
 * R4 사용처 = RiderInternalController.assign 후속 동기화 호출 (dead config 회피).</p>
 *
 * <p>R6 외부 endpoint 진입 시 같은 client 재사용 (결정 #26 압축률 1:2~3 박제 일관).</p>
 */
@FeignClient(name = "main-service", url = "${MAIN_SERVICE_URL:http://localhost:8080}")
@ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignClient")
public interface MainInternalClient {

    /**
     * 배달 상태 변경 알림 — interfaces.md §2.1.
     * Main이 orders.delivery_state 매핑 (ADR-004 line 86-96) 후 UPDATE.
     */
    @PutMapping("/internal/order/{orderId}/delivery-status")
    DeliveryStatusUpdateRes updateDeliveryStatus(
            @PathVariable("orderId") Long orderId,
            @RequestBody DeliveryStatusUpdateReq req);

    /**
     * 배달 완료 처리 — interfaces.md §2.2.
     * Main이 orders.delivery_state=3 (DELIVERED 종결, ADR-004) + orders.order_state=6 (CLAUDE.md §7 종결) 동반 UPDATE.
     * complete 호출 시점에 별 {@code updateDeliveryStatus(DELIVERED)} 중복 호출 X (Q-A4-호출 흐름 (나)).
     */
    @PostMapping("/internal/order/{orderId}/complete")
    DeliveryCompleteRes complete(
            @PathVariable("orderId") Long orderId,
            @RequestBody DeliveryCompleteReq req);
}
