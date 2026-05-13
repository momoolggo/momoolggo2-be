package com.green.mmg.rider.rider;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.rider.rider.model.RiderProfileReq;
import com.green.mmg.rider.rider.model.RiderProfileRes;
import com.green.mmg.rider.work.WorkSessionService;
import com.green.mmg.rider.work.dto.WorkSessionStatusReq;
import com.green.mmg.rider.work.dto.WorkSessionStatusRes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rider")
@RequiredArgsConstructor
public class RiderController {

    private final RiderService riderService;
    private final WorkSessionService workSessionService;

    /**
     * PUT /api/rider/profile — 가입 직후 라이더 추가정보 등록 (ADR-001 Q1-C).
     *
     * <p>callerUserNo는 SecurityContextHolder에서 추출 — req body의 userNo 신뢰 X.
     * D11 auto-approve true이면 가입 직후 ACTIVE 자동 전환.</p>
     */
    @PutMapping("/profile")
    public ResultResponse<RiderProfileRes> joinProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody RiderProfileReq req
    ) {
        RiderProfileRes data = riderService.joinProfile(principal.getSignedUserNo(), req);
        return new ResultResponse<>("프로필 등록 성공", data);
    }

    /** GET /api/rider/me — 본인 프로필 조회 */
    @GetMapping("/me")
    public ResultResponse<RiderProfileRes> getMe(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RiderProfileRes data = riderService.findProfile(principal.getSignedUserNo());
        return new ResultResponse<>("조회 성공", data);
    }

    /**
     * PUT /api/rider/status — R8 ACTIVE↔EATING 토글 (REQ-RDR-004, D8-a).
     *
     * <p>ACTIVE 첫 진입 시 work_session 자동 생성. EATING 진입 시 break 시작 시각 메모리 보관.</p>
     */
    @PutMapping("/status")
    public ResultResponse<WorkSessionStatusRes> toggleStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody WorkSessionStatusReq req
    ) {
        WorkSessionStatusRes data = workSessionService.toggleStatus(principal.getSignedUserNo(), req.to());
        return new ResultResponse<>("상태 전환 성공", data);
    }
}
