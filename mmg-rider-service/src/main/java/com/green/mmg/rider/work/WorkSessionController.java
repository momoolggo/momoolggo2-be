package com.green.mmg.rider.work;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.rider.work.dto.WorkSessionEndRes;
import com.green.mmg.rider.work.dto.WorkSessionSummaryRes;
import com.green.mmg.rider.work.dto.WorkSessionTodayRes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * R8 라이더 근무 세션 controller — 3 endpoint (Figma 170153/170202).
 *
 * <p>R6 RiderOrderController 분리 패턴 일관 (decision-#31 (가)). 토글(PUT /api/rider/status)은
 * RiderController에 위치 — Q-Endpoint-Path (가) 채택.</p>
 */
@RestController
@RequestMapping("/api/rider/work-session")
@RequiredArgsConstructor
public class WorkSessionController {

    private final WorkSessionService workSessionService;

    /** POST /api/rider/work-session/end — 업무 종료 (REQ-RDR-005, D9-a). */
    @PostMapping("/end")
    public ResultResponse<WorkSessionEndRes> endSession(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        WorkSessionEndRes data = workSessionService.endWorkSession(principal.getSignedUserNo());
        return new ResultResponse<>("업무 종료 성공", data);
    }

    /** GET /api/rider/work-session/today — Figma 170202 근무관리 카드. */
    @GetMapping("/today")
    public ResultResponse<WorkSessionTodayRes> getToday(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        WorkSessionTodayRes data = workSessionService.getTodaySession(principal.getSignedUserNo());
        return new ResultResponse<>("오늘 근무 조회 성공", data);
    }

    /** GET /api/rider/work-session/summary?period=today|week — 오늘/주간 합계. */
    @GetMapping("/summary")
    public ResultResponse<WorkSessionSummaryRes> getSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "period", defaultValue = "today") String period
    ) {
        WorkSessionSummaryRes data = workSessionService.getSummary(principal.getSignedUserNo(), period);
        return new ResultResponse<>("근무 합계 조회 성공", data);
    }
}
