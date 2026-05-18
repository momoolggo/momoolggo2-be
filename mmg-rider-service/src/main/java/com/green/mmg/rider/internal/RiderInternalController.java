package com.green.mmg.rider.internal;

import com.green.mmg.rider.delivery.DeliveryService;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.feign.MainInternalClient;
import com.green.mmg.rider.feign.dto.DeliveryStatusUpdateReq;
import com.green.mmg.rider.internal.dto.RiderApproveReq;
import com.green.mmg.rider.internal.dto.RiderInternalAssignReq;
import com.green.mmg.rider.internal.dto.RiderInternalAssignRes;
import com.green.mmg.rider.internal.dto.RiderInternalLocationRes;
import com.green.mmg.rider.internal.dto.RiderInternalMonitorRes;
import com.green.mmg.rider.internal.dto.RiderInternalNoticeReq;
import com.green.mmg.rider.internal.dto.RiderInternalNoticeRes;
import com.green.mmg.rider.internal.dto.RiderInternalStatusRes;
import com.green.mmg.rider.internal.dto.RiderSuspendReq;
import com.green.mmg.rider.location.LocationService;
import com.green.mmg.rider.notice.NoticeService;
import com.green.mmg.rider.notice.model.Notice;
import com.green.mmg.rider.rider.RiderService;
import com.green.mmg.rider.rider.model.RiderProfileRes;
import com.green.mmg.rider.rider.model.RiderStatus;
import com.green.mmg.rider.settlement.SettlementService;
import com.green.mmg.rider.settlement.dto.CalculateReq;
import com.green.mmg.rider.settlement.dto.ConfirmReq;
import com.green.mmg.rider.settlement.dto.SettlementRowRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/internal/rider")
@RequiredArgsConstructor
public class RiderInternalController {

    private final DeliveryService deliveryService;
    private final LocationService locationService;
    private final NoticeService noticeService;
    private final MainInternalClient mainInternalClient;
    private final SettlementService settlementService;
    private final RiderService riderService;  // Group 8.5 §3.1/§3.2 신설

    /**
     * 배차 요청 — interfaces.md §1.1 (case-#33-후속 정정, Q-A9.a (β+δ)).
     * req.riderNo null/0 = 라이더 풀 (WAITING_ASSIGN), 명시 = 강제 배차 (ASSIGNED).
     * Main 동기화는 ASSIGNED 시점만 (라이더 풀은 main이 호출자라 자동 인지, 라이더 수락 시점에 R6 흐름이 동기화).
     */
    @PostMapping("/assign")
    public RiderInternalAssignRes assign(@RequestBody RiderInternalAssignReq req) {
        RiderInternalAssignRes res = deliveryService.assignDelivery(req);

        if (res.riderNo() != null) {
            try {
                mainInternalClient.updateDeliveryStatus(req.orderId(),
                        new DeliveryStatusUpdateReq(
                                DeliveryStatus.ASSIGNED.name(),
                                res.riderNo(),
                                res.assignedAt()));
            } catch (Exception e) {
                log.warn("Main 동기화 실패 (배차는 성공): orderId={}, deliveryNo={}, ex={}",
                        req.orderId(), res.deliveryNo(), e.getMessage());
            }
        }

        return res;
    }

    @GetMapping("/{riderNo}/location")
    public RiderInternalLocationRes location(@PathVariable Long riderNo) {
        return locationService.getInternalLocation(riderNo);
    }

    /**
     * Admin 배달 관제 — TTL 살아있는 모든 라이더 위치 다건 조회 (Group 10, 2026-05-17).
     * 결정 (가) Redis TTL 기준. 빈 결과는 빈 List 반환.
     */
    @GetMapping("/locations/active")
    public List<RiderInternalLocationRes> activeLocations() {
        return locationService.getActiveLocations();
    }

    @GetMapping("/{riderNo}/status")
    public RiderInternalStatusRes status(@PathVariable Long riderNo) {
        return deliveryService.getRiderInternalStatus(riderNo);
    }

    /** Admin 모니터 — summary 4그룹 카운트 + status 필터 + page 목록. */
    @GetMapping("/monitor")
    public RiderInternalMonitorRes monitor(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page) {
        return deliveryService.getMonitor(status, page);
    }

    /** Admin 공지 작성 — 즉시(NOW) 또는 예약(RESERVED) 발송. */
    @PostMapping("/notice")
    public RiderInternalNoticeRes notice(@RequestBody RiderInternalNoticeReq req) {
        noticeService.createNotice(req);
        return RiderInternalNoticeRes.success();
    }

    /** Admin 공지 목록 조회 */
    @GetMapping("/notice")
    public List<Notice> getNoticeList() {
        return noticeService.getNoticeList();
    }

    /** Admin 공지 수정 */
    @PutMapping("/notice/{noticeId}")
    public RiderInternalNoticeRes updateNotice(
            @PathVariable Long noticeId,
            @RequestBody RiderInternalNoticeReq req) {
        noticeService.updateNotice(noticeId, req);
        return RiderInternalNoticeRes.success();
    }

    /** Admin 공지 삭제 */
    @DeleteMapping("/notice/{noticeId}")
    public RiderInternalNoticeRes deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return RiderInternalNoticeRes.success();
    }

    // ─── R7 정산 (admin Feign 호출용) ──────────────────────────

    /** Admin 주간 정산 집계 트리거 (D10-b). 멱등 처리. */
    @PostMapping("/settlement/calculate")
    public List<SettlementRowRes> calculateSettlement(@RequestBody CalculateReq req) {
        return settlementService.calculate(req.periodStart(), req.periodEnd());
    }

    /** Admin PENDING → CONFIRMED. */
    @PostMapping("/settlement/{settlementNo}/confirm")
    public SettlementRowRes confirmSettlement(
            @PathVariable Long settlementNo,
            @RequestBody ConfirmReq req) {
        return settlementService.confirm(settlementNo, req.adminNo());
    }

    /** Admin 모니터 — PENDING 목록. */
    @GetMapping("/settlement/pending")
    public List<SettlementRowRes> pendingSettlements() {
        return settlementService.findPending();
    }

    // ─── §3.1/§3.2 라이더 관리 (Group 8.5 신설, Q-A1 (라+)) ──────

    /**
     * 라이더 승인 — interfaces.md §3.1. PENDING → ACTIVE 전이 (Q-A20 (가) entity 검증).
     * {@code req.approvedByAdminNo}는 audit log 별 영역 (Q-A18 (b) Phase 6+ outbox 위임) — 본 단계 미사용.
     * Phase 6+ audit log 도입 시 req 인자 service 메서드로 전달 연결.
     */
    @PostMapping("/{riderNo}/approve")
    public RiderProfileRes approve(@PathVariable Long riderNo,
                                   @RequestBody RiderApproveReq req) {
        return riderService.approveRider(riderNo);
    }

    /**
     * 라이더 제재 — interfaces.md §3.2. ?→SUSPENDED 전이 (Q-A20 (가) entity 검증).
     * {@code reason}/{@code untilAt}은 본 단계 수신 + log.info만 (audit log Phase 6+ 위임).
     * Phase 6+ scheduler 도입 시 untilAt 인자 활용 (자동 SUSPENDED 해제).
     */
    @PostMapping("/{riderNo}/suspend")
    public RiderProfileRes suspend(@PathVariable Long riderNo,
                                   @RequestBody RiderSuspendReq req) {
        return riderService.suspendRider(riderNo, req.reason());
    }

    /**
     * 라이더 목록 조회 — interfaces.md §3.5 (Q-A1 (라++) Group 8 신설 2026-05-17).
     * {@code status} null이면 전체, 명시되면 4값 enum 필터. 학원 발표 MVP List 반환 (case-#36 자가 정정).
     */
    @GetMapping("/list")
    public List<RiderProfileRes> list(@RequestParam(required = false) RiderStatus status) {
        return riderService.listRiders(status);
    }
}