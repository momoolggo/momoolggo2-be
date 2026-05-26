package com.green.mmg.main.attendance;

import com.green.mmg.main.attendance.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    /** 본인 출석 일자 내림차순 (streak 계산용) */
    List<AttendanceLog> findByUserNoOrderByAttendanceDateDesc(Long userNo);

    /** 이번달 출석 일자 (캘린더 + monthCount 동시 사용) */
    List<AttendanceLog> findByUserNoAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long userNo, LocalDate from, LocalDate to);

    /** 오늘 출석 여부 (UPSERT idempotency 일관 — race 회피용) */
    Optional<AttendanceLog> findByUserNoAndAttendanceDate(Long userNo, LocalDate attendanceDate);
}
