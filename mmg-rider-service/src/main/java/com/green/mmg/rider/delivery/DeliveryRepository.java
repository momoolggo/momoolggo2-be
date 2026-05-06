package com.green.mmg.rider.delivery;

import com.green.mmg.rider.delivery.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 배달 Repository — R2 범위 = 기본 CRUD만.
 *
 * <p>R3 DeliveryService 진입 시 메서드명 추론 / @Query 추가 예정:
 * <ul>
 *   <li>{@code findByRiderNo(Long riderNo)} — 라이더별 배달 조회 (R6 외부 endpoint)</li>
 *   <li>{@code findByOrderId(String orderId)} — 주문별 배달 조회 (R4 Main → Rider Feign)</li>
 *   <li>{@code findByStatus(DeliveryStatus status)} — 가용 라이더 배차 (R4 WAITING_ASSIGN 검색)</li>
 * </ul>
 * 인덱스 3건(idx_delivery_rider_no / idx_delivery_order_id / idx_delivery_status)은 DDL 박제 완료.</p>
 */
public interface DeliveryRepository extends JpaRepository<Delivery, String> {
}
