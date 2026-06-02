package com.green.mmg.admin.settlement.controller;

import com.green.mmg.admin.common.enums.SettlementTargetType;
import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.settlement.repository.SettlementRepository;
import com.green.mmg.admin.settlement.service.SettlementService;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/settlement")
@RequiredArgsConstructor
public class InternalSettlementController {

    private final SettlementRepository settlementRepository;
    private final SettlementService settlementService;

    @Transactional(readOnly = true)
    @GetMapping("/stores/unpaid/exists")
    public ResultResponse<Boolean> hasUnpaidStoreSettlement(@RequestParam("storeIds") List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return new ResultResponse<>("미정산 확인 완료", false);
        }

        Long count = settlementRepository.countByTargetTypeAndTargetNoInAndStatusIn(
                SettlementTargetType.STORE,
                storeIds,
                List.of(SettlementsStatus.PENDING, SettlementsStatus.HELD)
        );

        return new ResultResponse<>("미정산 확인 완료", count != null && count > 0);
    }

    @GetMapping("/store/{storeId}")
    public ResultResponse<?> getSettlementsByStore(@PathVariable Long storeId) {
        return new ResultResponse<>("조회 성공", settlementService.getSettlementsByStoreId(storeId));
    }

    @GetMapping("/{settlementId}/orders")
    public ResultResponse<?> getSettlementOrders(@PathVariable Long settlementId) {
        return new ResultResponse<>("조회 성공", settlementService.getSettlementOrders(settlementId));
    }

    @PatchMapping("/{settlementId}/bank-account")
    public ResultResponse<?> updateBankAccount(
            @PathVariable Long settlementId,
            @RequestParam String bankAccount) {
        settlementService.updateBankAccount(settlementId, bankAccount);
        return new ResultResponse<>("계좌 정보 변경 완료", null);
    }
}
