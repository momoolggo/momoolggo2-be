package com.green.mmg.admin.settlement.service;

import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.common.enums.SettlementTargetType;
import com.green.mmg.admin.dto.feign.InternalStoreListPageRes;
import com.green.mmg.admin.dto.feign.InternalStoreListRes;
import com.green.mmg.admin.feign.MainFeignClient;
import com.green.mmg.admin.settlement.dto.SettlementReq;
import com.green.mmg.admin.settlement.dto.SettlementRes;
import com.green.mmg.admin.settlement.dto.SettlementSummaryRes;
import com.green.mmg.admin.settlement.entity.Settlement;
import com.green.mmg.admin.settlement.repository.SettlementRepository;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final MainFeignClient mainFeignClient;

    // storeId → storeName 맵 생성
    private Map<Long, String> getStoreNameMap() {
        try {
            InternalStoreListPageRes res = mainFeignClient.getStoreList(0, 200, null).getResultData();
            if (res == null || res.getContent() == null) return Map.of();
            return res.getContent().stream()
                    .collect(Collectors.toMap(InternalStoreListRes::getStoreId, InternalStoreListRes::getStoreName));
        } catch (Exception e) {
            return Map.of();
        }
    }

    // Settlement → SettlementRes 변환 (targetName 포함)
    private SettlementRes toRes(Settlement s, Map<Long, String> storeNameMap) {
        String targetName = null;
        if (s.getTargetType() == SettlementTargetType.STORE) {
            targetName = storeNameMap.getOrDefault(s.getTargetNo(), "가게 #" + s.getTargetNo());
        } else if (s.getTargetType() == SettlementTargetType.RIDER) {
            targetName = "라이더 #" + s.getTargetNo();
        }
        return new SettlementRes(
                s.getSettlementId(), s.getTargetType(), s.getTargetNo(), targetName,
                s.getPeriodStart(), s.getPeriodEnd(), s.getItemCount(),
                s.getGrossAmount(), s.getFeeAmount(), s.getTaxAmount(),
                s.getOtherDeduction(), s.getNetAmount(), s.getStatus(),
                s.getPaidAt(), s.getBankAccount(), s.getCreatedAt()
        );
    }

    // 정산 요약
    public SettlementSummaryRes getSummary() {
        Integer expectedAmount = settlementRepository.sumExpectedAmount();
        Integer completedAmount = settlementRepository.sumCompletedAmount();
        Long completedCount = settlementRepository.countByStatusIn(
                List.of(SettlementsStatus.DONE, SettlementsStatus.COMPLETED));
        Long pendingCount = settlementRepository.countByStatus(SettlementsStatus.PENDING);
        return new SettlementSummaryRes(expectedAmount, completedAmount, completedCount, pendingCount);
    }

    // 정산 목록 조회 (가게 탭 전용 - STORE만, 페이지네이션)
    public Map<String, Object> getSettlementList(SettlementReq req) {
        Map<Long, String> storeNameMap = getStoreNameMap();
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());
        List<Settlement> list;
        long totalCount;

        if (req.getStatus() != null) {
            // COMPLETED 선택 시 DONE도 같이 조회
            if (req.getStatus() == SettlementsStatus.COMPLETED) {
                list = settlementRepository.findByTargetTypeAndStatusIn(
                        SettlementTargetType.STORE,
                        List.of(SettlementsStatus.COMPLETED, SettlementsStatus.DONE),
                        pageable);
                totalCount = settlementRepository.countByTargetTypeAndStatusIn(
                        SettlementTargetType.STORE,
                        List.of(SettlementsStatus.COMPLETED, SettlementsStatus.DONE));
            } else {
                list = settlementRepository.findByTargetTypeAndStatus(SettlementTargetType.STORE, req.getStatus(), pageable);
                totalCount = settlementRepository.countByTargetTypeAndStatus(SettlementTargetType.STORE, req.getStatus());
            }
        } else if (req.getStartDate() != null && req.getEndDate() != null) {
            list = settlementRepository.findByTargetTypeAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
                    SettlementTargetType.STORE, req.getStartDate(), req.getEndDate(), pageable);
            totalCount = list.size();
        } else {
            list = settlementRepository.findByTargetType(SettlementTargetType.STORE, pageable);
            totalCount = settlementRepository.countByTargetType(SettlementTargetType.STORE);
        }

        List<SettlementRes> content = list.stream().map(s -> toRes(s, storeNameMap)).toList();
        int totalPages = (int) Math.ceil((double) totalCount / req.getSize());

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("content", content);
        result.put("totalCount", totalCount);
        result.put("totalPages", Math.max(totalPages, 1));
        result.put("currentPage", req.getPage());
        return result;
    }

    // 정산 상세 조회
    public SettlementRes getSettlementDetail(Long settlementId) {
        Settlement s = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("정산 정보를 찾을 수 없습니다."));
        Map<Long, String> storeNameMap = getStoreNameMap();
        return toRes(s, storeNameMap);
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

    public List<SettlementRes> getSettlementsByStoreId(Long storeId) {
        Map<Long, String> storeNameMap = getStoreNameMap();
        return settlementRepository.findByTargetTypeAndTargetNo(SettlementTargetType.STORE, storeId)
                .stream().map(s -> toRes(s, storeNameMap)).toList();
    }
}