package com.green.mmg.rider.notice;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.internal.dto.RiderInternalNoticeReq;
import com.green.mmg.rider.notice.model.Notice;
import com.green.mmg.rider.notice.model.NoticeCategory;
import com.green.mmg.rider.notice.model.NoticeSendType;
import com.green.mmg.rider.notice.model.NoticeTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 공지 도메인 서비스 — POST /internal/rider/notice (즉시/예약 발송).
 *
 * <p>R9 진입 전 부분 구현 — 작성(POST)만. 갱신(PUT)/삭제(DELETE)/조회(GET)는 R9에서 도입.
 * R3 정착 패턴 일관 (BusinessException + e.getStatus() 동적 매핑, mmg-common 미수정).</p>
 *
 * <p>임시 채워 넣기 (R9 / Admin Feign 진입 시 정정):
 * <ul>
 *   <li>{@code senderAdminNo}: {@link #TEMP_SENDER_ADMIN_NO} 하드코딩 (X-Admin-No 헤더 전달 미도입)</li>
 *   <li>{@code category}: {@link NoticeCategory#GENERAL} 하드코딩 (body 미박제)</li>
 * </ul></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final long TEMP_SENDER_ADMIN_NO = 1L;
    private static final NoticeCategory TEMP_CATEGORY = NoticeCategory.GENERAL;

    private final NoticeRepository noticeRepository;

    @Transactional
    public Notice createNotice(RiderInternalNoticeReq req) {
        validate(req);

        LocalDateTime publishedAt = req.sendType() == NoticeSendType.NOW
                ? LocalDateTime.now()
                : req.reservedAt();
        LocalDateTime reservedAt = req.sendType() == NoticeSendType.RESERVED
                ? req.reservedAt()
                : null;

        Notice notice = new Notice(
                TEMP_CATEGORY,
                req.title(),
                req.content(),
                req.targetType(),
                req.sendType(),
                reservedAt,
                publishedAt,
                TEMP_SENDER_ADMIN_NO);
        return noticeRepository.save(notice);
    }

    private void validate(RiderInternalNoticeReq req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new BusinessException("title은 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        if (req.content() == null || req.content().isBlank()) {
            throw new BusinessException("content는 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        if (req.targetType() == null) {
            throw new BusinessException("targetType은 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        if (req.sendType() == null) {
            throw new BusinessException("sendType은 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        if (req.sendType() == NoticeSendType.RESERVED) {
            if (req.reservedAt() == null) {
                throw new BusinessException(
                        "RESERVED 발송은 reservedAt이 필수입니다.",
                        HttpStatus.BAD_REQUEST);
            }
            if (!req.reservedAt().isAfter(LocalDateTime.now())) {
                throw new BusinessException(
                        "reservedAt은 현재 시각 이후여야 합니다.",
                        HttpStatus.BAD_REQUEST);
            }
        }
    }
}
