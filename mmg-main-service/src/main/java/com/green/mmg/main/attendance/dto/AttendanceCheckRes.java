package com.green.mmg.main.attendance.dto;

import java.time.LocalDate;

/**
 * 2026-05-25 9건 트랙 #8 부채 Step B — 출석 체크 응답.
 */
public record AttendanceCheckRes(
        LocalDate date,
        boolean alreadyChecked,  // true = 이미 오늘 출석 완료 (idempotent)
        int streak,
        int monthCount
) {
}
