package com.green.mmg.rider.notice.sse;

import com.green.mmg.rider.notice.dto.RiderNoticeRowRes;

/**
 * 신규 공지 발행 이벤트 — NoticeService.createNotice(sendType=NOW)에서 발행.
 * SettlementUpdatedEvent 박제 일관 (record + AFTER_COMMIT 리스너).
 *
 * <p>RESERVED 발송은 본 이벤트 발행 X (published_at 미래라 라이더 GET 필터에 자동 제외).
 * 미래 시각 도래 시 자동 트리거는 Phase 6+ scheduler tech-debt.</p>
 */
public record NoticeBroadcastEvent(RiderNoticeRowRes payload) {
}
