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
import com.green.mmg.rider.location.LocationService;
import com.green.mmg.rider.notice.NoticeService;
import com.green.mmg.rider.notice.model.Notice;
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

    @PostMapping("/{riderNo}/assign")
    public RiderInternalAssignRes assign(
            @PathVariable Long riderNo,
            @RequestBody RiderInternalAssignReq req) {
        RiderInternalAssignRes res = deliveryService.assignDelivery(riderNo, req);

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
        return locationService.getInternalLocation(riderNo);
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
}