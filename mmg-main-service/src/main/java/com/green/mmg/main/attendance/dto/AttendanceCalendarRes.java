package com.green.mmg.main.attendance.dto;

import java.util.List;

/**
 * 2026-05-25 9건 트랙 #8 부채 Step B — 출석 캘린더 응답.
 * attendedDays = 해당 월의 출석 일자 (1~31).
 */
public record AttendanceCalendarRes(
        int year,
        int month,
        List<Integer> attendedDays,
        int monthCount,
        int streak
) {
}
