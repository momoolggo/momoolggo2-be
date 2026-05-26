package com.green.mmg.main.attendance;

import com.green.mmg.main.attendance.dto.AttendanceCalendarRes;
import com.green.mmg.main.attendance.dto.AttendanceCheckRes;
import com.green.mmg.main.attendance.entity.AttendanceLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 2026-05-25 9건 트랙 #8 부채 Step B — 출석 시스템.
 *
 * <p>3 책임: 출석 체크 (idempotent UPSERT) / streak 계산 (오늘 기준 역순 연속 일수) / monthCount (이번달 횟수).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceLogRepository attendanceLogRepository;

    /**
     * 오늘 출석 체크 (idempotent — 이미 체크되면 그대로 OK).
     * UNIQUE 제약 race 시 DataIntegrityViolationException 무음 (이미 출석 완료로 처리).
     */
    @Transactional
    public AttendanceCheckRes checkToday(Long userNo) {
        LocalDate today = LocalDate.now();
        boolean alreadyChecked = attendanceLogRepository.findByUserNoAndAttendanceDate(userNo, today).isPresent();
        if (!alreadyChecked) {
            try {
                attendanceLogRepository.save(new AttendanceLog(userNo, today));
            } catch (DataIntegrityViolationException race) {
                // 동시 race — 다른 트랜잭션이 INSERT 성공. 출석 완료로 처리.
                log.warn("attendance_log UNIQUE race 회피 — userNo={}, date={}", userNo, today);
                alreadyChecked = true;
            }
        }
        int streak = calculateStreak(userNo, today);
        int monthCount = countMonth(userNo, today);
        return new AttendanceCheckRes(today, alreadyChecked, streak, monthCount);
    }

    /** 이번달 출석 캘린더 + monthCount + streak */
    @Transactional(readOnly = true)
    public AttendanceCalendarRes getCalendar(Long userNo, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.plusMonths(1).minusDays(1);
        List<AttendanceLog> logs = attendanceLogRepository
                .findByUserNoAndAttendanceDateBetweenOrderByAttendanceDateAsc(userNo, from, to);
        List<Integer> attendedDays = logs.stream()
                .map(l -> l.getAttendanceDate().getDayOfMonth())
                .toList();
        int streak = calculateStreak(userNo, LocalDate.now());
        return new AttendanceCalendarRes(year, month, attendedDays, attendedDays.size(), streak);
    }

    /** 오늘부터 역순으로 연속 출석 일수 (오늘 출석 안 했어도 어제부터 카운트). */
    @Transactional(readOnly = true)
    public int calculateStreak(Long userNo, LocalDate today) {
        List<AttendanceLog> logs = attendanceLogRepository.findByUserNoOrderByAttendanceDateDesc(userNo);
        if (logs.isEmpty()) return 0;
        // 오늘 출석 있으면 오늘부터, 없으면 어제부터 시작점
        LocalDate cursor = logs.get(0).getAttendanceDate().equals(today) ? today : today.minusDays(1);
        // 오늘도 어제도 출석 없으면 streak=0
        if (!logs.get(0).getAttendanceDate().equals(cursor)) return 0;
        int streak = 0;
        for (AttendanceLog log : logs) {
            if (log.getAttendanceDate().equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    /** 이번달 출석 횟수 */
    @Transactional(readOnly = true)
    public int countMonth(Long userNo, LocalDate ref) {
        LocalDate from = ref.withDayOfMonth(1);
        LocalDate to = from.plusMonths(1).minusDays(1);
        return attendanceLogRepository
                .findByUserNoAndAttendanceDateBetweenOrderByAttendanceDateAsc(userNo, from, to)
                .size();
    }
}
