package com.green.mmg.rider.work;

import com.green.mmg.rider.work.model.WorkSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 근무 세션 Repository — R8 진입 시 메서드명 추론 추가.
 *
 * <p>인덱스 1건(idx_work_session_rider_no)은 DDL 박제 완료.</p>
 */
public interface WorkSessionRepository extends JpaRepository<WorkSession, Long> {

    /** 진행 중(ended_at IS NULL) 세션 조회 — 토글/종료 시점 단일 행 검증. */
    Optional<WorkSession> findByRiderNoAndEndedAtIsNull(Long riderNo);

    /** 기간 내 세션 (오늘/주간 summary). started_at 기준 BETWEEN + DESC 정렬 (W-1 fix). */
    List<WorkSession> findByRiderNoAndStartedAtBetweenOrderByStartedAtDesc(
            Long riderNo, LocalDateTime from, LocalDateTime to);
}
