package com.green.mmg.main.attendance;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.attendance.dto.AttendanceCalendarRes;
import com.green.mmg.main.attendance.dto.AttendanceCheckRes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 2026-05-25 9건 트랙 #8 부채 Step B — 출석 시스템 외부 endpoint.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** 오늘 출석 체크 (idempotent — 이미 체크되어도 200 OK + alreadyChecked=true) */
    @PostMapping("/check")
    public ResultResponse<AttendanceCheckRes> check(@AuthenticationPrincipal UserPrincipal principal) {
        return new ResultResponse<>("출석 체크 완료",
                attendanceService.checkToday(principal.getSignedUserNo()));
    }

    /** 이번달 출석 캘린더 — year/month 미지정 시 이번달 기본 */
    @GetMapping("/calendar")
    public ResultResponse<AttendanceCalendarRes> calendar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {
        LocalDate ref = LocalDate.now();
        int y = year != null ? year : ref.getYear();
        int m = month != null ? month : ref.getMonthValue();
        return new ResultResponse<>("출석 캘린더 조회 완료",
                attendanceService.getCalendar(principal.getSignedUserNo(), y, m));
    }
}
