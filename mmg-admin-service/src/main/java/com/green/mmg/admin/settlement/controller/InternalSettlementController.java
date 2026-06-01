package com.green.mmg.admin.settlement.controller;

import com.green.mmg.admin.common.enums.SettlementTargetType;
import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.settlement.repository.SettlementRepository;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/settlement")
@RequiredArgsConstructor
public class InternalSettlementController {

    private final SettlementRepository settlementRepository;

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
}
