package com.green.mmg.rider.delivery;

import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
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

    /** monitor summary — 그룹별 카운트 (idx_delivery_status 활용). */
    long countByStatus(DeliveryStatus status);

    /** monitor deliveries — status 그룹 필터 (idx_delivery_status 활용). */
    Page<Delivery> findByStatusIn(Collection<DeliveryStatus> statuses, Pageable pageable);

    /** R6 라이더 측 진행 중 목록 — riderNo 본인 + status 그룹 (idx_delivery_rider_no + status). */
    List<Delivery> findByRiderNoAndStatusInOrderByAssignedAtDesc(
            Long riderNo, Collection<DeliveryStatus> statuses);

    /** R6 대기 배달 목록 — WAITING_ASSIGN 전체, 가용 라이더 모두 대상 (rider_no NULL). */
    List<Delivery> findByStatusOrderByCreatedAtAsc(DeliveryStatus status);

    /**
     * R9 배달내역 — 본인 DELIVERED 목록, deliveredAt 기간 필터 + DESC 정렬.
     * REQ-RDR-003 "기간 필터 지원" 박제 일관 (assigned_at 아닌 delivered_at 기준 = 완료 시점).
     */
    List<Delivery> findByRiderNoAndStatusAndDeliveredAtBetweenOrderByDeliveredAtDesc(
            Long riderNo, DeliveryStatus status, LocalDateTime from, LocalDateTime to);
}
