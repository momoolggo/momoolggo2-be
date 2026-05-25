package com.green.mmg.main.pet;

import com.green.mmg.main.pet.entity.GreenPointLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GreenPointLogRepository extends JpaRepository<GreenPointLog, Long> {
    boolean existsByOrderId(Long orderId);

    /** 2026-05-25 9건 트랙 #8 부채 — 사용자 누적 그린포인트 합산 (펫 페이지 표시용) */
    @Query("SELECT COALESCE(SUM(g.point), 0) FROM GreenPointLog g WHERE g.userNo = :userNo")
    long sumByUserNo(@Param("userNo") Long userNo);
}
