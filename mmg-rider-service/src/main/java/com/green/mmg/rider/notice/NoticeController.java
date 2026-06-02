package com.green.mmg.rider.notice;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.rider.notice.dto.RiderNoticeRowRes;
import com.green.mmg.rider.notice.sse.NoticeSseRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * R9 라이더 공지 controller — interfaces.md No.90 / REQ-RDR-006.
 *
 * <p>R6 RiderOrderController 분리 패턴 일관 (decision-#31 (가)). RIDER role 인증 필수.
 * 작성/수정/삭제는 admin-service 진입점 (RiderInternalController.notice). 라이더 측은 GET만.</p>
 */
@RestController
@RequestMapping("/api/rider/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeSseRegistry sseRegistry;

    /** SSE stream timeout — SettlementController 박제 일관 (30분, 클라이언트 EventSource 자동 재연결). */
    private static final long SSE_TIMEOUT_MS = 30L * 60 * 1000;

    @GetMapping
    public ResultResponse<List<RiderNoticeRowRes>> getNotices(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<RiderNoticeRowRes> data = noticeService.getRiderNoticeList();
        return new ResultResponse<>("공지 목록 조회 성공", data);
    }

    /**
     * GET /api/rider/notice/stream — 신규 공지 SSE 푸시 (2026-05-28 트랙).
     *
     * <p>admin이 NoticeService.createNotice(sendType=NOW)로 작성 시 트랜잭션 commit 후 본 emitter에 push.
     * SettlementController.stream 박제 일관 (userNo별 단일 emitter, 재접속 시 기존 close).</p>
     *
     * <p>키 비대칭 메모 — Settlement는 riderNo로 등록(집계 기준), Notice는 userNo로 등록.
     * broadcast 타겟이 전체 emitter라 riderNo 조회 불필요 (W-2 정합).</p>
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal principal) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        return sseRegistry.register(principal.getSignedUserNo(), emitter);
    }
}
