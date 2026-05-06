package com.green.mmg.admin.settlement.service;

import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.common.enums.SettlementTargetType;
import com.green.mmg.admin.settlement.dto.SettlementReq;
import com.green.mmg.admin.settlement.dto.SettlementSummaryRes;
import com.green.mmg.admin.settlement.entity.Settlement;
import com.green.mmg.admin.settlement.repository.SettlementRepository;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;

    // 정산 요약
    public SettlementSummaryRes getSummary() {
        Integer expectedAmount = settlementRepository.sumExpectedAmount();
        Integer completedAmount = settlementRepository.sumCompletedAmount();
        Long pendingCount = settlementRepository.countByStatus(SettlementsStatus.PENDING);
        return new SettlementSummaryRes(expectedAmount, completedAmount, pendingCount);
    }

    // 정산 목록 조회
    public List<Settlement> getSettlementList(SettlementReq req) {
        if (req.getTargetType() != null) {
            return settlementRepository.findByTargetType(req.getTargetType());
        }
        if (req.getStatus() != null) {
            return settlementRepository.findByStatus(req.getStatus());
        }
        if (req.getStartDate() != null && req.getEndDate() != null) {
            return settlementRepository
                    .findByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
                            req.getStartDate(), req.getEndDate());
        }
        return settlementRepository.findAll();
    }

    // 정산 상세 조회
    public Settlement getSettlementDetail(Long settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("정산 정보를 찾을 수 없습니다."));
    }

    // 정산 완료 처리
    @Transactional
    public void completeSettlement(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("정산 정보를 찾을 수 없습니다."));
        settlement.complete();
    }

    // 정산 보류 처리
    @Transactional
    public void holdSettlement(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("정산 정보를 찾을 수 없습니다."));
        settlement.hold();
    }
}