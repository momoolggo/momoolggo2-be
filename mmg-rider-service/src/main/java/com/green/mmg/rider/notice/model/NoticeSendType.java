package com.green.mmg.rider.notice.model;

/**
 * 공지 발송 방식 — POST /internal/rider/notice body.
 *
 * <ul>
 *   <li>{@link #NOW}: 즉시 발송 (published_at = now)</li>
 *   <li>{@link #RESERVED}: 예약 발송 (published_at = reservedAt, 미래값 검증 필수)</li>
 * </ul>
 */
public enum NoticeSendType {
    NOW,
    RESERVED
}
