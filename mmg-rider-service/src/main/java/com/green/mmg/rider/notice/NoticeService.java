package com.green.mmg.rider.notice;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.internal.dto.RiderInternalNoticeReq;
import com.green.mmg.rider.notice.dto.RiderNoticeRowRes;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

    /** R9 라이더 가시성 — Q-Visibility (가) ALL + RIDER (SPECIFIC은 admin POST에서 block). */
    private static final Set<NoticeTargetType> RIDER_VISIBLE_TARGETS =
            EnumSet.of(NoticeTargetType.ALL, NoticeTargetType.RIDER);

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

    // 공지 목록 조회 — admin 전체 (R4-a, KYL)
    public List<Notice> getNoticeList() {
        return noticeRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * R9 라이더 측 GET /api/rider/notice — 가시성 필터 (Q-Visibility (가)).
     *
     * <p>target_type IN (ALL, RIDER) AND published_at <= now() ORDER BY published_at DESC.
     * 예약(RESERVED) 발송 중 시각 미도래 항목은 자동 제외.</p>
     */
    @Transactional(readOnly = true)
    public List<RiderNoticeRowRes> getRiderNoticeList() {
        return noticeRepository
                .findByTargetTypeInAndPublishedAtLessThanEqualOrderByPublishedAtDesc(
                        RIDER_VISIBLE_TARGETS, LocalDateTime.now())
                .stream()
                .map(RiderNoticeRowRes::from)
                .toList();
    }

    // 공지 수정
    @Transactional
    public void updateNotice(Long noticeId, RiderInternalNoticeReq req) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException("공지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        notice.update(req.title(), req.content(), req.sendType(), req.reservedAt());
    }

    // 공지 삭제
    @Transactional
    public void deleteNotice(Long noticeId) {
        noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException("공지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        noticeRepository.deleteById(noticeId);
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
        // SPECIFIC은 target_user_no 컬럼 + 라우팅 로직 부재 — R9 도입 예정 (reviewer W-2)
        if (req.targetType() == NoticeTargetType.SPECIFIC) {
            throw new BusinessException(
                    "SPECIFIC 타겟은 R9 도입 예정입니다. 현재 미지원.",
                    HttpStatus.BAD_REQUEST);
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