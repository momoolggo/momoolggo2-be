package com.green.mmg.rider.settlement;

import com.green.mmg.rider.settlement.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 정산 Repository — R2 범위 = 기본 CRUD만.
 *
 * <p>R7 SettlementService 진입 시 메서드명 추론 / @Query 추가 예정:
 * <ul>
 *   <li>{@code findByRiderNoOrderByPeriodStartDesc(Long riderNo)} — 라이더별 정산 내역 (이번주 PENDING + 과거 CONFIRMED)</li>
 *   <li>{@code findByStatusAndPeriodEndBefore(SettlementStatus, LocalDate)} — admin PENDING 목록</li>
 *   <li>{@code findByRiderNoAndPeriodStart(Long, LocalDate)} — 특정 주 정산 조회 (중복 INSERT 방지)</li>
 * </ul>
 * 인덱스 1건(idx_settlement_rider_no)은 DDL 박제 완료.</p>
 */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
}
