package com.green.mmg.admin.feign;

import com.green.mmg.admin.dto.feign.RiderInternalMonitorRes;
import com.green.mmg.admin.dto.feign.RiderInternalNoticeReq;
import com.green.mmg.admin.dto.feign.RiderInternalNoticeRes;
import com.green.mmg.admin.dto.feign.RiderNoticeRes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mmg-rider-service",
        url = "${feign.rider-service.url:http://localhost:8084}")
public interface RiderFeignClient {

    /** 라이더 관제 데이터 조회 — GET /internal/rider/monitor */
    @GetMapping("/internal/rider/monitor")
    RiderInternalMonitorRes getMonitor(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page
    );

    /** 라이더 전체 공지 발송 — POST /internal/rider/notice */
    @PostMapping("/internal/rider/notice")
    RiderInternalNoticeRes sendNotice(@RequestBody RiderInternalNoticeReq req);

    /** 공지 목록 조회 */
    @GetMapping("/internal/rider/notice")
    List<RiderNoticeRes> getNoticeList();

    /** 공지 수정 */
    @PutMapping("/internal/rider/notice/{noticeId}")
    RiderInternalNoticeRes updateNotice(@PathVariable("noticeId") Long noticeId,
                                        @RequestBody RiderInternalNoticeReq req);

    /** 공지 삭제 */
    @DeleteMapping("/internal/rider/notice/{noticeId}")
    RiderInternalNoticeRes deleteNotice(@PathVariable("noticeId") Long noticeId);
}