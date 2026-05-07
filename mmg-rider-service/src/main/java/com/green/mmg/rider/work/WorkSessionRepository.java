package com.green.mmg.rider.work;

import com.green.mmg.rider.work.model.WorkSession;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 근무 세션 Repository — R2 범위 = 기본 CRUD만.
 *
 * <p>R3 WorkSessionService 진입 시 메서드명 추론 / @Query 추가 예정:
 * <ul>
 *   <li>{@code findByRiderNoAndEndedAtIsNull(Long riderNo)} — 진행 중 세션 조회 (D9 업무 종료 시점)</li>
 *   <li>{@code findByRiderNoAndStartedAtBetween(Long riderNo, LocalDateTime, LocalDateTime)} — 주간 합계 조회</li>
 * </ul>
 * 인덱스 1건(idx_work_session_rider_no)은 DDL 박제 완료.</p>
 */
public interface WorkSessionRepository extends JpaRepository<WorkSession, Long> {
}
