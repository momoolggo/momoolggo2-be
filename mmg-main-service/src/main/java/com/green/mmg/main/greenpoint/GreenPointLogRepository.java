package com.green.mmg.main.greenpoint;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GreenPointLogRepository extends JpaRepository<GreenPointLog, Long> {
    boolean existsByOrderId(Long orderId);

    List<GreenPointLog> findByStatusInAndRetryCountLessThanOrderByGreenPointLogIdAsc(
            List<String> statuses,
            Integer retryCount,
            Pageable pageable
    );

    /** 2026-05-25 9건 트랙 #8 — 사용자 누적 그린포인트 합산 (펫 페이지 표시용) */
    @Query("SELECT COALESCE(SUM(g.point), 0) FROM GreenPointLog g WHERE g.userNo = :userNo")
    long sumByUserNo(@Param("userNo") Long userNo);
}