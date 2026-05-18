package com.green.mmg.admin.settlement.repository;

import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.common.enums.SettlementTargetType;
import com.green.mmg.admin.settlement.entity.Settlement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // 대상 타입별 조회
    List<Settlement> findByTargetType(SettlementTargetType targetType);
    List<Settlement> findByTargetType(SettlementTargetType targetType, Pageable pageable);

    // 상태별 조회
    List<Settlement> findByStatus(SettlementsStatus status);

    // 기간별 조회
    List<Settlement> findByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            LocalDate startDate, LocalDate endDate);

    // 대기 건수
    Long countByStatus(SettlementsStatus status);

    // 완료 건수 (DONE + COMPLETED 둘 다)
    Long countByStatusIn(List<SettlementsStatus> statuses);

    // 완료 금액 합계
    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s WHERE s.status IN ('DONE', 'COMPLETED')")
    Integer sumCompletedAmount();

    // 예상 금액 합계
    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s WHERE s.status = 'PENDING'")
    Integer sumExpectedAmount();

    List<Settlement> findByTargetTypeAndTargetNo(SettlementTargetType targetType, Long targetNo);

    List<Settlement> findByTargetTypeAndStatus(SettlementTargetType targetType, SettlementsStatus status);
    List<Settlement> findByTargetTypeAndStatus(SettlementTargetType targetType, SettlementsStatus status, Pageable pageable);
    Long countByTargetTypeAndStatus(SettlementTargetType targetType, SettlementsStatus status);

    List<Settlement> findByTargetTypeAndStatusIn(SettlementTargetType targetType, List<SettlementsStatus> statuses, Pageable pageable);
    Long countByTargetTypeAndStatusIn(SettlementTargetType targetType, List<SettlementsStatus> statuses);

    Long countByTargetType(SettlementTargetType targetType);

    List<Settlement> findByTargetTypeAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            SettlementTargetType targetType, LocalDate startDate, LocalDate endDate);
    List<Settlement> findByTargetTypeAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            SettlementTargetType targetType, LocalDate startDate, LocalDate endDate, Pageable pageable);
}