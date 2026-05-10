package com.green.mmg.rider.delivery;

import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 배달 Repository — R2 기본 CRUD + R4 진입 시 진행 중 조회 메서드 추가.
 *
 * <p>인덱스 3건(idx_delivery_rider_no / idx_delivery_order_id / idx_delivery_status)은 DDL 박제 완료.</p>
 */
public interface DeliveryRepository extends JpaRepository<Delivery, String> {

    /**
     * 진행 중 배달 1건 조회 (RiderService.getInternalStatus, interfaces.md §1.3).
     * 가장 최근 배차된 진행 중 배달 — assigned_at DESC 정렬.
     * 진행 중 정의: WAITING_ASSIGN 제외 (배차 대기), DELIVERED 제외 (terminal).
     */
    Optional<Delivery> findFirstByRiderNoAndStatusInOrderByAssignedAtDesc(
            Long riderNo, List<DeliveryStatus> statuses);
}
