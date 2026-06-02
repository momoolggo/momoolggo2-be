package com.green.mmg.admin.settlement.service;

import com.green.mmg.admin.common.enums.SettlementsStatus;
import com.green.mmg.admin.common.enums.SettlementTargetType;
import com.green.mmg.admin.dto.feign.InternalSettlementOrderListRes;
import com.green.mmg.admin.dto.feign.InternalSettlementOrderRes;
import com.green.mmg.admin.dto.feign.InternalStoreListPageRes;
import com.green.mmg.admin.dto.feign.InternalStoreListRes;
import com.green.mmg.admin.feign.MainFeignClient;
import com.green.mmg.admin.settlement.dto.SettlementReq;
import com.green.mmg.admin.settlement.dto.SettlementRes;
import com.green.mmg.admin.settlement.dto.SettlementSummaryRes;
import com.green.mmg.admin.settlement.entity.Settlement;
import com.green.mmg.admin.settlement.repository.SettlementRepository;
import com.green.mmg.admin.settlement.payout.SettlementPayoutService;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final MainFeignClient mainFeignClient;
    private final SettlementPayoutService settlementPayoutService;

    // storeId → storeName 맵 생성
    private Map<Long, String> getStoreNameMap() {
        try {
            InternalStoreListPageRes res = mainFeignClient
                    .getStoreList(0, 200, null, null, null, null, null, null)
                    .getResultData();

            if (res == null || res.getContent() == null) return Map.of();

            return res.getContent().stream()
                    .collect(Collectors.toMap(
                            InternalStoreListRes::getStoreId,
                            InternalStoreListRes::getStoreName
                    ));
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

    // 정산 요약 (이번 주 기준 - periodEnd 월~일)
    public SettlementSummaryRes getSummary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        Long expectedAmount = settlementRepository.sumExpectedAmountThisWeek(SettlementsStatus.PENDING, weekStart, weekEnd);
        Long monthlyExpectedAmount = settlementRepository.sumCompletedAmountThisWeek(
                List.of(SettlementsStatus.values()), monthStart, monthEnd);
        Long completedAmount = settlementRepository.sumCompletedAmountThisWeek(List.of(SettlementsStatus.DONE, SettlementsStatus.COMPLETED), weekStart, weekEnd);
        Long completedCount = settlementRepository.countByStatusInAndPeriodEndBetween(
                List.of(SettlementsStatus.DONE, SettlementsStatus.COMPLETED), weekStart, weekEnd);
        Long pendingStoreCount = settlementRepository.countByTargetTypeAndStatusAndPeriodEndBetween(
                SettlementTargetType.STORE, SettlementsStatus.PENDING, weekStart, weekEnd);
        return new SettlementSummaryRes(expectedAmount, monthlyExpectedAmount, completedAmount, completedCount,
                pendingStoreCount, pendingStoreCount, 0L);
    }

    // 정산 목록 조회 (가게 탭 전용 - STORE만, 페이지네이션 + keyword 검색)
    public Map<String, Object> getSettlementList(SettlementReq req) {
        Map<Long, String> storeNameMap = getStoreNameMap();
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());

        // status 정규화: null=전체, COMPLETED → [COMPLETED, DONE]
        List<SettlementsStatus> statuses = normalizeStatuses(req.getStatus());

        // keyword → 매칭 가게 ID 목록 추출
        List<Long> matchingTargetNos = null;
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            String kw = req.getKeyword().toLowerCase();
            matchingTargetNos = storeNameMap.entrySet().stream()
                    .filter(e -> e.getValue().toLowerCase().contains(kw))
                    .map(Map.Entry::getKey)
                    .toList();
            if (matchingTargetNos.isEmpty()) {
                Map<String, Object> empty = new java.util.LinkedHashMap<>();
                empty.put("content", List.of());
                empty.put("totalCount", 0L);
                empty.put("totalPages", 1);
                empty.put("currentPage", req.getPage());
                return empty;
            }
        }

        LocalDate startDate = req.getStartDate() != null ? req.getStartDate() : LocalDate.of(2000, 1, 1);
        LocalDate endDate   = req.getEndDate()   != null ? req.getEndDate()   : LocalDate.of(2099, 12, 31);

        Page<Settlement> page = (matchingTargetNos != null)
                ? settlementRepository.searchByFiltersWithKeyword(
                        SettlementTargetType.STORE, statuses,
                        startDate, endDate,
                        matchingTargetNos, pageable)
                : settlementRepository.searchByFilters(
                        SettlementTargetType.STORE, statuses,
                        startDate, endDate, pageable);

        List<SettlementRes> content = page.getContent().stream()
                .map(s -> toRes(s, storeNameMap)).toList();

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("content", content);
        result.put("totalCount", page.getTotalElements());
        result.put("totalPages", Math.max(page.getTotalPages(), 1));
        result.put("currentPage", req.getPage());
        return result;
    }

    private List<SettlementsStatus> normalizeStatuses(SettlementsStatus status) {
        if (status == null) return List.of(SettlementsStatus.values());
        if (status == SettlementsStatus.COMPLETED) return List.of(SettlementsStatus.COMPLETED, SettlementsStatus.DONE);
        return List.of(status);
    }

    // 정산 상세 조회
    public SettlementRes getSettlementDetail(Long settlementId) {
        Settlement s = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("정산 정보를 찾을 수 없습니다."));
        Map<Long, String> storeNameMap = getStoreNameMap();
        return toRes(s, storeNameMap);
    }

    // 정산 완료 처리 + 토스페이먼츠 지급 요청
    @Transactional
    public void completeSettlement(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("정산 정보를 찾을 수 없습니다."));
        settlement.complete();
        // 토스페이먼츠 지급대행 API 호출 (설계 완료, 실운영 시 secret-key 설정 필요)
        try {
            settlementPayoutService.executePayout(settlementId);
        } catch (Exception e) {
            log.warn("지급 요청 실패 (정산 완료는 유지) settlementId={} error={}", settlementId, e.getMessage());
        }
    }

    // 보류 해제 (HELD → PENDING)
    @Transactional
    public void releaseHold(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("정산 정보를 찾을 수 없습니다."));
        settlement.release();
    }

    // 계좌 정보 변경
    @Transactional
    public void updateBankAccount(Long settlementId, String bankAccount) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("정산 정보를 찾을 수 없습니다."));
        if (settlement.getStatus() != SettlementsStatus.PENDING
                && settlement.getStatus() != SettlementsStatus.HELD) {
            throw new BusinessException("정산 완료된 건은 계좌 변경이 불가합니다.", HttpStatus.BAD_REQUEST);
        }
        settlement.updateBankAccount(bankAccount);
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

    // 정산 상세 주문내역 - 날짜별 그룹핑
    public Map<String, Object> getSettlementOrders(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("정산 정보를 찾을 수 없습니다."));

        try {
            InternalSettlementOrderListRes res = mainFeignClient.getSettlementOrders(
                    settlement.getTargetNo(),
                    settlement.getPeriodStart().toString(),
                    settlement.getPeriodEnd().toString()
            ).getResultData();

            if (res == null || res.getOrders() == null) {
                return Map.of("dailySales", List.of(), "totalSales", 0L);
            }

            // 날짜별 그룹핑
            Map<String, Long> dailySalesMap = new java.util.LinkedHashMap<>();
            Map<String, Integer> dailyCountMap = new java.util.LinkedHashMap<>();

            // 기간 내 모든 날짜 초기화 (0원으로)
            java.time.LocalDate cur = settlement.getPeriodStart();
            while (!cur.isAfter(settlement.getPeriodEnd())) {
                dailySalesMap.put(cur.toString(), 0L);
                dailyCountMap.put(cur.toString(), 0);
                cur = cur.plusDays(1);
            }

            // 주문 데이터 날짜별 집계
            for (InternalSettlementOrderRes order : res.getOrders()) {
                if (order.getOrderTime() == null) continue;
                String date = order.getOrderTime().toLocalDate().toString();
                dailySalesMap.merge(date, order.getOrderAmount() != null ? order.getOrderAmount() : 0L, Long::sum);
                dailyCountMap.merge(date, 1, Integer::sum);
            }

            // 날짜별 리스트로 변환
            List<Map<String, Object>> dailySales = dailySalesMap.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> row = new java.util.LinkedHashMap<>();
                        row.put("date", e.getKey());
                        row.put("amount", e.getValue());
                        row.put("count", dailyCountMap.getOrDefault(e.getKey(), 0));
                        return row;
                    }).toList();

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("dailySales", dailySales);
            result.put("totalSales", res.getTotalSales() != null ? res.getTotalSales() : 0L);
            result.put("periodStart", settlement.getPeriodStart().toString());
            result.put("periodEnd", settlement.getPeriodEnd().toString());
            return result;

        } catch (Exception e) {
            log.error("정산 주문 내역 조회 실패: settlementId={}, cause={}, message={}",
                    settlementId, e.getClass().getSimpleName(), e.getMessage(), e);
            return Map.of("dailySales", List.of(), "totalSales", 0L);
        }
    }
}
