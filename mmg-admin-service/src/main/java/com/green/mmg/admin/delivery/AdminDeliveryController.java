package com.green.mmg.admin.delivery;

import com.green.mmg.admin.dto.feign.RiderInternalMonitorRes;
import com.green.mmg.admin.dto.feign.RiderInternalNoticeReq;
import com.green.mmg.admin.dto.feign.RiderInternalNoticeRes;
import com.green.mmg.admin.dto.feign.RiderLocationRes;
import com.green.mmg.admin.dto.feign.RiderNoticeRes;
import com.green.mmg.admin.feign.RiderFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/delivery")
@RequiredArgsConstructor
public class AdminDeliveryController {

    private final RiderFeignClient riderFeignClient;

    /** 배달 관제 데이터 조회 */
    @GetMapping("/monitor")
    public RiderInternalMonitorRes getDeliveryMonitor(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page
    ) {
        return riderFeignClient.getMonitor(status, page);
    }

    /** 라이더 전체 공지 발송 */
    @PostMapping("/notice")
    public RiderInternalNoticeRes sendRiderNotice(
            @RequestBody RiderInternalNoticeReq req
    ) {
        return riderFeignClient.sendNotice(req);
    }

    /** 라이더 공지 목록 조회 */
    @GetMapping("/notice")
    public List<RiderNoticeRes> getNoticeList() {
        return riderFeignClient.getNoticeList();
    }

    /** 라이더 공지 수정 */
    @PutMapping("/notice/{noticeId}")
    public RiderInternalNoticeRes updateNotice(
            @PathVariable Long noticeId,
            @RequestBody RiderInternalNoticeReq req
    ) {
        return riderFeignClient.updateNotice(noticeId, req);
    }

    /** 라이더 공지 삭제 */
    @DeleteMapping("/notice/{noticeId}")
    public RiderInternalNoticeRes deleteNotice(@PathVariable Long noticeId) {
        return riderFeignClient.deleteNotice(noticeId);
    }

    @GetMapping("/rider-count")
    public ResponseEntity<?> getRiderCount() {
        return ResponseEntity.ok(Map.of("count", 0));
    }

    /** Admin 배달 관제 지도용 — TTL 살아있는 라이더 위치 다건 (Group 10, 2026-05-17). */
    @GetMapping("/rider-locations")
    public List<RiderLocationRes> getRiderLocations() {
        return riderFeignClient.getActiveRiderLocations();
    }
}