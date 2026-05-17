package com.green.mmg.rider.delivery;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.rider.delivery.dto.DeliveryCancelReq;
import com.green.mmg.rider.delivery.dto.DeliveryCompleteReq;
import com.green.mmg.rider.delivery.dto.DeliveryHistoryRes;
import com.green.mmg.rider.delivery.dto.DeliveryTransitionResult;
import com.green.mmg.rider.delivery.dto.DeliveryWaitingRowRes;
import com.green.mmg.rider.feign.MainInternalClient;
import com.green.mmg.rider.feign.dto.DeliveryStatusUpdateReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Phase 5-R6: 라이더 측 외부 endpoint — interfaces.md §6.2 (8 endpoint).
 *
 * <p>R5 LocationController 분리 패턴 일관 (decision-#31 (가)). RIDER role 인증 필수.</p>
 *
 * <p>6 transition endpoint(accept/reject/arrive/pickup/depart/complete) 모두 Main 동기화 후속 호출
 * (R4 RiderInternalController.assign 패턴 일관 — best-effort try-catch, 트랜잭션 외부).</p>
 *
 * <p>decision-#26 (a) 압축률 측정 — MainInternalClient 6회 재사용 (R3 1:4.5 패턴 일관 가능성 검증).</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/rider/order")
@RequiredArgsConstructor
public class RiderOrderController {

    private final DeliveryService deliveryService;
    private final MainInternalClient mainInternalClient;

    @GetMapping("/waiting")
    public ResultResponse<List<DeliveryWaitingRowRes>> waiting(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<DeliveryWaitingRowRes> data = deliveryService.getWaitingDeliveries(
                principal.getSignedUserNo());
        return new ResultResponse<>("대기 배달 조회 성공", data);
    }

    @GetMapping("/inprogress")
    public ResultResponse<List<DeliveryWaitingRowRes>> inProgress(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<DeliveryWaitingRowRes> data = deliveryService.getMyInProgressDeliveries(
                principal.getSignedUserNo());
        return new ResultResponse<>("진행 중 배달 조회 성공", data);
    }

    /**
     * R9 배달내역 — 본인 DELIVERED 목록 + 기간 필터 + 합계 (REQ-RDR-003).
     * Q-Period (가): from/to 미지정 시 최근 30일 자동.
     */
    @GetMapping("/completed")
    public ResultResponse<DeliveryHistoryRes> completed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DeliveryHistoryRes data = deliveryService.getMyCompletedDeliveries(
                principal.getSignedUserNo(), from, to);
        return new ResultResponse<>("배달내역 조회 성공", data);
    }

    @PutMapping("/{deliveryNo}/accept")
    public ResultResponse<Void> accept(
            @PathVariable String deliveryNo,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeliveryTransitionResult result = deliveryService.acceptDelivery(
                deliveryNo, principal.getSignedUserNo());
        notifyMain(result);
        return new ResultResponse<>("배차 수락 성공", null);
    }

    @PutMapping("/{deliveryNo}/reject")
    public ResultResponse<Void> reject(
            @PathVariable String deliveryNo,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeliveryTransitionResult result = deliveryService.rejectDelivery(
                deliveryNo, principal.getSignedUserNo());
        notifyMain(result);
        return new ResultResponse<>("배차 반려 성공", null);
    }

    @PutMapping("/{deliveryNo}/arrive")
    public ResultResponse<Void> arrive(
            @PathVariable String deliveryNo,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeliveryTransitionResult result = deliveryService.arriveAtStore(
                deliveryNo, principal.getSignedUserNo());
        notifyMain(result);
        return new ResultResponse<>("가게 도착 처리 성공", null);
    }

    @PutMapping("/{deliveryNo}/pickup")
    public ResultResponse<Void> pickup(
            @PathVariable String deliveryNo,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeliveryTransitionResult result = deliveryService.pickup(
                deliveryNo, principal.getSignedUserNo());
        notifyMain(result);
        return new ResultResponse<>("픽업 완료 처리 성공", null);
    }

    @PutMapping("/{deliveryNo}/depart")
    public ResultResponse<Void> depart(
            @PathVariable String deliveryNo,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeliveryTransitionResult result = deliveryService.depart(
                deliveryNo, principal.getSignedUserNo());
        notifyMain(result);
        return new ResultResponse<>("이동 시작 처리 성공", null);
    }

    @PutMapping("/{deliveryNo}/complete")
    public ResultResponse<Void> complete(
            @PathVariable String deliveryNo,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DeliveryCompleteReq req) {
        DeliveryTransitionResult result = deliveryService.completeDelivery(
                deliveryNo, principal.getSignedUserNo(), req);
        notifyMainComplete(deliveryNo, result, req);
        return new ResultResponse<>("배달 완료 처리 성공", null);
    }

    /**
     * R6-cancel: 라이더가 진행 중 배달을 사고/개인사유/기타로 반려.
     * Main 동기화 시 reason 전달 X (decision-#35 (가)) — status=WAITING_ASSIGN 알림만.
     * PUT 일관 (R6 6 transition endpoint와 동일, reviewer CW-2 정정).
     */
    @PutMapping("/{deliveryNo}/cancel")
    public ResultResponse<Void> cancel(
            @PathVariable String deliveryNo,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DeliveryCancelReq req) {
        DeliveryTransitionResult result = deliveryService.cancelDelivery(
                deliveryNo, principal.getSignedUserNo(), req);
        notifyMain(result);
        return new ResultResponse<>("배달 반려 처리 성공", null);
    }

    /**
     * Main 동기화 — best-effort try-catch (R4 RiderInternalController.assign 일관).
     * 동기화 실패해도 배달 상태 변경은 성공 (보상은 Phase 6 outbox 검토).
     */
    private void notifyMain(DeliveryTransitionResult result) {
        try {
            mainInternalClient.updateDeliveryStatus(
                    result.orderId(),
                    new DeliveryStatusUpdateReq(
                            result.newStatus().name(),
                            result.riderNo(),
                            result.changedAt()));
        } catch (Exception e) {
            log.warn("Main 동기화 실패: orderId={}, status={}, ex={}",
                    result.orderId(), result.newStatus(), e.getMessage());
        }
    }

    /**
     * Main 완료 동기화 — interfaces.md §2.2. best-effort try-catch (notifyMain 패턴 일관, D1-bis).
     *
     * <p>complete 호출이 {@code delivery_state=3} + {@code order_state=6} 동반 UPDATE이라
     * {@code updateDeliveryStatus(DELIVERED)} 중복 호출 X (Q-A4-호출 흐름 (나)).
     * Feign DTO는 외부 endpoint DTO와 같은 클래스명이라 full qualified name 사용 (case-#34 영역 분리 일관).</p>
     *
     * <p>매핑: {@code deliveryNo}는 path variable에서, {@code riderNo}+{@code completedAt}는 result에서,
     * {@code deliveredMethod}+{@code deliveredPhotoUrl}는 외부 req에서 보강.</p>
     */
    private void notifyMainComplete(String deliveryNo,
                                    DeliveryTransitionResult result,
                                    DeliveryCompleteReq externalReq) {
        try {
            mainInternalClient.complete(
                    result.orderId(),
                    new com.green.mmg.rider.feign.dto.DeliveryCompleteReq(
                            deliveryNo,
                            result.riderNo(),
                            externalReq.deliveredMethod(),
                            externalReq.deliveredPhotoUrl(),
                            result.changedAt()));
        } catch (Exception e) {
            log.warn("Main 완료 동기화 실패: orderId={}, deliveryNo={}, ex={}",
                    result.orderId(), deliveryNo, e.getMessage());
        }
    }
}
