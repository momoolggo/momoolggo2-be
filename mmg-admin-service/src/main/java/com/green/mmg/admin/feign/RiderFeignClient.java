package com.green.mmg.admin.feign;

import com.green.mmg.admin.dto.feign.RiderInternalMonitorRes;
import com.green.mmg.admin.dto.feign.RiderInternalNoticeReq;
import com.green.mmg.admin.dto.feign.RiderInternalNoticeRes;
import com.green.mmg.admin.dto.feign.RiderNoticeRes;
import com.green.mmg.admin.dto.feign.RiderSettlementCalculateReq;
import com.green.mmg.admin.dto.feign.RiderSettlementConfirmReq;
import com.green.mmg.admin.dto.feign.RiderSettlementRowRes;
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
        url = "${feign.rider-service.url:http://localhost:8082}")
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

    // ─── R7 정산 (Group 5 신설, team-handoff §9 부채 해소) ──────────────────────
    // Q-A2 (다) path 일관: rider Provider /internal/rider/settlement/* 박제 따름.
    // Q-A10.c (a) 기존 client 재사용 (dead config 회피 + Group 1.5 패턴 일관).
    // case-#34 강제: rider Provider 시그니처 1:1 일관 (List<RiderSettlementRowRes> / RiderSettlementRowRes 반환).

    /** 주간 정산 집계 트리거 (D10-b 멱등). rider Provider {@code List<SettlementRowRes>} 반환 일관. */
    @PostMapping("/internal/rider/settlement/calculate")
    List<RiderSettlementRowRes> calculateRiderSettlement(@RequestBody RiderSettlementCalculateReq req);

    /** PENDING → CONFIRMED. rider Provider {@code SettlementRowRes} 반환 일관 (분류 B 자율 정정 — void X). */
    @PostMapping("/internal/rider/settlement/{settlementNo}/confirm")
    RiderSettlementRowRes confirmRiderSettlement(@PathVariable("settlementNo") Long settlementNo,
                                                 @RequestBody RiderSettlementConfirmReq req);

    /** Admin 모니터 — PENDING 목록 조회. */
    @GetMapping("/internal/rider/settlement/pending")
    List<RiderSettlementRowRes> getRiderSettlementPending();
}