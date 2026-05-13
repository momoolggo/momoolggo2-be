package com.green.mmg.rider.notice.dto;

import com.green.mmg.rider.notice.model.Notice;
import com.green.mmg.rider.notice.model.NoticeCategory;

import java.time.LocalDateTime;

/**
 * GET /api/rider/notice 응답 row — R9.
 *
 * <p>Figma 170158 박제 — category(IMPORTANT/SAFETY/GENERAL) UI 색상 매핑 FE 책임 (ADR-009 line 154).</p>
 */
public record RiderNoticeRowRes(
        Long noticeNo,
        NoticeCategory category,
        String title,
        String content,
        LocalDateTime publishedAt
) {
    public static RiderNoticeRowRes from(Notice n) {
        return new RiderNoticeRowRes(
                n.getNoticeNo(),
                n.getCategory(),
                n.getTitle(),
                n.getContent(),
                n.getPublishedAt()
        );
    }
}
