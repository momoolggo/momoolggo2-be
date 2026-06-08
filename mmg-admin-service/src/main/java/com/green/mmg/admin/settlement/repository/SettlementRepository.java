package com.green.mmg.admin.settlement.repository;

import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.common.enums.SettlementTargetType;
import com.green.mmg.admin.settlement.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 이번 주 완료 금액 합계 (periodEnd 기준 월~일)
    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s WHERE s.status IN :statuses AND s.periodEnd BETWEEN :weekStart AND :weekEnd")
    Long sumCompletedAmountThisWeek(@Param("statuses") List<SettlementsStatus> statuses, @Param("weekStart") LocalDate weekStart, @Param("weekEnd") LocalDate weekEnd);

    // 이번 주 예상 금액 합계 (periodEnd 기준 월~일 — completedAmount 등 다른 카드와 기준 통일)
    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s WHERE s.status = :status AND s.periodEnd BETWEEN :weekStart AND :weekEnd")
    Long sumExpectedAmountThisWeek(@Param("status") SettlementsStatus status, @Param("weekStart") LocalDate weekStart, @Param("weekEnd") LocalDate weekEnd);

    // 이번 주 완료 건수
    Long countByStatusInAndPeriodEndBetween(List<SettlementsStatus> statuses, LocalDate weekStart, LocalDate weekEnd);

    // 이번 주 가게 대기 건수 (periodEnd 기준)
    Long countByTargetTypeAndStatusAndPeriodEndBetween(SettlementTargetType targetType, SettlementsStatus status, LocalDate weekStart, LocalDate weekEnd);

    // 이번 주 가게 대기 건수 (periodStart 기준)
    Long countByTargetTypeAndStatusAndPeriodStartBetween(SettlementTargetType targetType, SettlementsStatus status, LocalDate weekStart, LocalDate weekEnd);

    List<Settlement> findByTargetTypeAndTargetNo(SettlementTargetType targetType, Long targetNo);

    List<Settlement> findByTargetTypeAndStatus(SettlementTargetType targetType, SettlementsStatus status);
    List<Settlement> findByTargetTypeAndStatus(SettlementTargetType targetType, SettlementsStatus status, Pageable pageable);
    Long countByTargetTypeAndStatus(SettlementTargetType targetType, SettlementsStatus status);

    List<Settlement> findByTargetTypeAndStatusIn(SettlementTargetType targetType, List<SettlementsStatus> statuses, Pageable pageable);
    Long countByTargetTypeAndStatusIn(SettlementTargetType targetType, List<SettlementsStatus> statuses);
    Long countByTargetTypeAndTargetNoInAndStatusIn(SettlementTargetType targetType, List<Long> targetNos, List<SettlementsStatus> statuses);

    Long countByTargetType(SettlementTargetType targetType);

    List<Settlement> findByTargetTypeAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            SettlementTargetType targetType, LocalDate startDate, LocalDate endDate);
    List<Settlement> findByTargetTypeAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            SettlementTargetType targetType, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 통합 검색 쿼리 (status 복수 + 기간 optional)
    @Query(value = "SELECT s FROM Settlement s WHERE s.targetType = :targetType " +
                   "AND s.status IN :statuses " +
                   "AND s.periodStart >= :startDate " +
                   "AND s.periodEnd <= :endDate " +
                   "ORDER BY CASE s.status " +
                   "WHEN com.green.mmg.admin.common.enums.SettlementsStatus.PENDING THEN 0 " +
                   "WHEN com.green.mmg.admin.common.enums.SettlementsStatus.HELD THEN 1 " +
                   "ELSE 2 END ASC, s.periodEnd DESC, s.settlementId DESC",
           countQuery = "SELECT COUNT(s) FROM Settlement s WHERE s.targetType = :targetType " +
                        "AND s.status IN :statuses " +
                        "AND s.periodStart >= :startDate " +
                        "AND s.periodEnd <= :endDate")
    Page<Settlement> searchByFilters(
            @Param("targetType") SettlementTargetType targetType,
            @Param("statuses") List<SettlementsStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 통합 검색 쿼리 + keyword (targetNo IN 조건 추가)
    @Query(value = "SELECT s FROM Settlement s WHERE s.targetType = :targetType " +
                   "AND s.status IN :statuses " +
                   "AND s.periodStart >= :startDate " +
                   "AND s.periodEnd <= :endDate " +
                   "AND s.targetNo IN :targetNos " +
                   "ORDER BY CASE s.status " +
                   "WHEN com.green.mmg.admin.common.enums.SettlementsStatus.PENDING THEN 0 " +
                   "WHEN com.green.mmg.admin.common.enums.SettlementsStatus.HELD THEN 1 " +
                   "ELSE 2 END ASC, s.periodEnd DESC, s.settlementId DESC",
           countQuery = "SELECT COUNT(s) FROM Settlement s WHERE s.targetType = :targetType " +
                        "AND s.status IN :statuses " +
                        "AND s.periodStart >= :startDate " +
                        "AND s.periodEnd <= :endDate " +
                        "AND s.targetNo IN :targetNos")
    Page<Settlement> searchByFiltersWithKeyword(
            @Param("targetType") SettlementTargetType targetType,
            @Param("statuses") List<SettlementsStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("targetNos") List<Long> targetNos,
            Pageable pageable);
}
