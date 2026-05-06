package com.green.mmg.admin.settlement.repository;

import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.common.enums.SettlementTargetType;
import com.green.mmg.admin.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // 대상 타입별 조회
    List<Settlement> findByTargetType(SettlementTargetType targetType);

    // 상태별 조회
    List<Settlement> findByStatus(SettlementsStatus status);

    // 기간별 조회
    List<Settlement> findByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            LocalDate startDate, LocalDate endDate);

    // 대기 건수
    Long countByStatus(SettlementsStatus status);

    // 완료 금액 합계
    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s WHERE s.status = 'DONE'")
    Integer sumCompletedAmount();

    // 예상 금액 합계
    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s WHERE s.status = 'PENDING'")
    Integer sumExpectedAmount();
}