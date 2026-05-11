package com.green.mmg.rider.internal.dto;

import com.green.mmg.rider.notice.model.NoticeSendType;
import com.green.mmg.rider.notice.model.NoticeTargetType;

import java.time.LocalDateTime;

/**
 * Admin → Rider 공지 작성 Body — POST /internal/rider/notice.
 *
 * <p>{@code reservedAt}: sendType=RESERVED 시 필수 + 미래값 검증 (NoticeService).
 * sendType=NOW 시 무시 (published_at = now).</p>
 */
public record RiderInternalNoticeReq(
        String title,
        NoticeTargetType targetType,
        String content,
        NoticeSendType sendType,
        LocalDateTime reservedAt
) {
}
