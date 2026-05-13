package com.green.mmg.rider.notice;

import com.green.mmg.rider.notice.model.Notice;
import com.green.mmg.rider.notice.model.NoticeTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 공지사항 Repository — R2 + R9 라이더 가시성 메서드 추가.
 *
 * <p>인덱스 1건(idx_notice_published_at)은 DDL 박제 완료.</p>
 */
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** Admin 전체 목록 — KYL 추가 (R4-a 시점). */
    List<Notice> findAllByOrderByCreatedAtDesc();

    /**
     * R9 라이더 가시성 필터 — target_type IN (ALL, RIDER) AND published_at <= now()
     * ORDER BY published_at DESC. SPECIFIC은 미지원 (NoticeService.validate에서 block).
     */
    List<Notice> findByTargetTypeInAndPublishedAtLessThanEqualOrderByPublishedAtDesc(
            Collection<NoticeTargetType> targetTypes, LocalDateTime now);
}
