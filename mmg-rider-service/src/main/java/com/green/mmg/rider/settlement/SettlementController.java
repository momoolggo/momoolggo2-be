package com.green.mmg.rider.settlement;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.rider.settlement.dto.AccountReq;
import com.green.mmg.rider.settlement.dto.AccountRes;
import com.green.mmg.rider.settlement.dto.SettlementRowRes;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * R7 라이더 정산 controller — Figma 170202 박제 일관.
 *
 * <p>R6 RiderOrderController / R8 WorkSessionController 분리 패턴 일관 (decision-#31 (가)).
 * RIDER role 인증 필수.</p>
 */
@RestController
@RequestMapping("/api/rider/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * GET /api/rider/settlement?from=&to= — 본인 정산 내역.
     * 기본 최근 12주 (REQ-RDR-003 "기간 필터 지원" + Figma 170202 이번주 박제).
     */
    @GetMapping
    public ResultResponse<List<SettlementRowRes>> history(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<SettlementRowRes> data = settlementService.findByRiderAndPeriod(
                principal.getSignedUserNo(), from, to);
        return new ResultResponse<>("정산 내역 조회 성공", data);
    }

    /** GET /api/rider/settlement/account — 본인 정산 계좌 조회 */
    @GetMapping("/account")
    public ResultResponse<AccountRes> getAccount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AccountRes data = settlementService.getAccount(principal.getSignedUserNo());
        return new ResultResponse<>("정산 계좌 조회 성공", data);
    }

    /** PUT /api/rider/settlement/account — 본인 정산 계좌 변경 (Q-AccountChange (가) 자유 변경) */
    @PutMapping("/account")
    public ResultResponse<AccountRes> updateAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AccountReq req
    ) {
        AccountRes data = settlementService.updateAccount(principal.getSignedUserNo(), req);
        return new ResultResponse<>("정산 계좌 변경 성공", data);
    }
}
