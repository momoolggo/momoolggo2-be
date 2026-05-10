package com.green.mmg.rider.internal;

import com.green.mmg.rider.delivery.DeliveryService;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.feign.MainInternalClient;
import com.green.mmg.rider.feign.dto.DeliveryStatusUpdateReq;
import com.green.mmg.rider.internal.dto.RiderInternalAssignReq;
import com.green.mmg.rider.internal.dto.RiderInternalAssignRes;
import com.green.mmg.rider.internal.dto.RiderInternalLocationRes;
import com.green.mmg.rider.internal.dto.RiderInternalMonitorRes;
import com.green.mmg.rider.internal.dto.RiderInternalNoticeReq;
import com.green.mmg.rider.internal.dto.RiderInternalNoticeRes;
import com.green.mmg.rider.internal.dto.RiderInternalStatusRes;
import com.green.mmg.rider.notice.NoticeService;
import com.green.mmg.rider.rider.RiderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Main/Admin → Rider Internal API — interfaces.md §1.1~1.3.
 *
 * <p>Phase 4-A {@code InternalUserController} 정착 패턴 일관 — {@code @RestController + /internal/...} prefix.
 * RiderSecurityConfig {@code /internal/**} permitAll (Phase 4-B Gateway 차단으로 외부 노출 0).
 * X-Internal 헤더 검증은 Phase 6+ 강화 예정.</p>
 *
 * <p>3 endpoint:
 * <ul>
 *   <li>{@code POST /internal/rider/{riderNo}/assign} — 배차 요청 + Main 동기화 (best-effort)</li>
 *   <li>{@code GET /internal/rider/{riderNo}/location} — 위치 조회 (R4 stub, R5 진입 시 Redis 채움)</li>
 *   <li>{@code GET /internal/rider/{riderNo}/status} — 라이더 상태 + 진행 중 배달</li>
 * </ul></p>
 *
 * <p>Main 동기화 흐름 (ADR-004 line 144-148 박제 일관) — DeliveryService.assignDelivery 트랜잭션 커밋 후
 * 별도 호출 (트랜잭션 외부). best-effort: 동기화 실패해도 배차는 성공 (보상은 Phase 6 outbox 패턴 검토).</p>
 */
@Slf4j
@RestController
@RequestMapping("/internal/rider")
@RequiredArgsConstructor
public class RiderInternalController {

    private final DeliveryService deliveryService;
    private final RiderService riderService;
    private final NoticeService noticeService;
    private final MainInternalClient mainInternalClient;

    @PostMapping("/{riderNo}/assign")
    public RiderInternalAssignRes assign(
            @PathVariable Long riderNo,
            @RequestBody RiderInternalAssignReq req) {
        RiderInternalAssignRes res = deliveryService.assignDelivery(riderNo, req);

        // Main 동기화 (best-effort, 트랜잭션 외부 — ADR-004 line 144-148 박제 일관)
        try {
            mainInternalClient.updateDeliveryStatus(req.orderId(),
                    new DeliveryStatusUpdateReq(
                            DeliveryStatus.ASSIGNED.name(),
                            riderNo,
                            res.assignedAt()));
        } catch (Exception e) {
            log.warn("Main 동기화 실패 (배차는 성공): orderId={}, deliveryNo={}, ex={}",
                    req.orderId(), res.deliveryNo(), e.getMessage());
        }

        return res;
    }

    @GetMapping("/{riderNo}/location")
    public RiderInternalLocationRes location(@PathVariable Long riderNo) {
        return riderService.getInternalLocation(riderNo);
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
}
