package com.green.mmg.admin.settlement.controller;

import com.green.mmg.admin.common.enums.SettlementTargetType;
import com.green.mmg.admin.dto.feign.RiderSettlementConfirmReq;
import com.green.mmg.admin.feign.RiderFeignClient;
import com.green.mmg.admin.settlement.dto.SettlementReq;
import com.green.mmg.admin.settlement.dto.SettlementRes;
import com.green.mmg.admin.settlement.service.SettlementService;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final RiderFeignClient riderFeignClient;

    // 정산 요약
    @GetMapping("/summary")
    public ResultResponse<?> getSummary() {
        return new ResultResponse<>("조회 성공", settlementService.getSummary());
    }

    // 정산 목록 조회
    @GetMapping
    public ResultResponse<?> getSettlementList(
            @ModelAttribute SettlementReq req) {
        return new ResultResponse<>("조회 성공",
                settlementService.getSettlementList(req));
    }

    // 정산 상세 조회
    @GetMapping("/{settlementId}")
    public ResultResponse<?> getSettlementDetail(
            @PathVariable Long settlementId) {
        return new ResultResponse<>("조회 성공",
                settlementService.getSettlementDetail(settlementId));
    }

    // 정산 완료 처리
    @PatchMapping("/{settlementId}/complete")
    public ResultResponse<?> completeSettlement(
            @PathVariable Long settlementId) {
        settlementService.completeSettlement(settlementId);
        return new ResultResponse<>("정산 완료 처리", null);
    }

    // 정산 보류 처리
    @PatchMapping("/{settlementId}/hold")
    public ResultResponse<?> holdSettlement(
            @PathVariable Long settlementId) {
        settlementService.holdSettlement(settlementId);
        return new ResultResponse<>("정산 보류 처리", null);
    }

    // 라이더 정산 PENDING 목록
    @GetMapping("/rider")
    public ResultResponse<?> getRiderSettlements() {
        return new ResultResponse<>("라이더 정산 목록", riderFeignClient.getRiderSettlementPending());
    }

    // 라이더 정산 확정
    @PostMapping("/rider/{settlementNo}/confirm")
    public ResultResponse<?> confirmRiderSettlement(@PathVariable Long settlementNo) {
        return new ResultResponse<>("정산 확정", riderFeignClient.confirmRiderSettlement(settlementNo, new RiderSettlementConfirmReq(1L)));
    }

    // 가게별 정산 내역 조회 (internal)
    @GetMapping("/internal/store/{storeId}")
    public ResultResponse<?> getSettlementsByStore(@PathVariable Long storeId) {
        return new ResultResponse<>("조회 성공", settlementService.getSettlementsByStoreId(storeId));
    }


}