package com.green.mmg.admin.delivery;

import com.green.mmg.admin.dto.feign.RiderInternalMonitorRes;
import com.green.mmg.admin.dto.feign.RiderInternalNoticeReq;
import com.green.mmg.admin.dto.feign.RiderInternalNoticeRes;
import com.green.mmg.admin.feign.RiderFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/rider-count")
    public ResponseEntity<?> getRiderCount() {
        return ResponseEntity.ok(Map.of("count", 0));
    }
}