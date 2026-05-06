package com.green.mmg.rider.delivery;

import com.green.mmg.rider.delivery.model.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 배달 이력 Repository — R2 범위 = 기본 CRUD만.
 *
 * <p>R3 DeliveryService 진입 시 메서드명 추론 예정:
 * <ul>
 *   <li>{@code findByDeliveryNoOrderByChangedAtAsc(String deliveryNo)} — 특정 delivery 이력 시간순 조회</li>
 * </ul>
 * 인덱스 1건({@code idx_delivery_log_delivery_no}) DDL 박제 완료.</p>
 */
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {
}
