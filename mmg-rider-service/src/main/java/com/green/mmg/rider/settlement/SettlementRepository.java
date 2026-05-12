package com.green.mmg.rider.settlement;

import com.green.mmg.rider.settlement.model.Settlement;
import com.green.mmg.rider.settlement.model.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 정산 Repository — R2 + R7 라이더/admin 조회 메서드 추가.
 * 인덱스 1건(idx_settlement_rider_no) DDL 박제 완료.
 */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /** 라이더별 정산 내역 (이번주 PENDING + 과거 CONFIRMED). DESC 정렬. */
    List<Settlement> findByRiderNoOrderByPeriodStartDesc(Long riderNo);

    /** 라이더별 기간 필터 (Figma 170202 기간 필터). */
    List<Settlement> findByRiderNoAndPeriodStartBetweenOrderByPeriodStartDesc(
            Long riderNo, LocalDate from, LocalDate to);

    /** 특정 주 정산 조회 (중복 INSERT 방지 — calculate 호출 시 멱등 처리). */
    Optional<Settlement> findByRiderNoAndPeriodStartAndPeriodEnd(
            Long riderNo, LocalDate periodStart, LocalDate periodEnd);

    /** Admin PENDING 모니터 — status 필터. */
    List<Settlement> findByStatusOrderByPeriodStartDesc(SettlementStatus status);
}
