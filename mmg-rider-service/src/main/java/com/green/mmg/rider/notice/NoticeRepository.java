package com.green.mmg.rider.notice;

import com.green.mmg.rider.notice.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 공지사항 Repository — R2 범위 = 기본 CRUD만.
 *
 * <p>R9 NoticeService 진입 시 메서드명 추론 / @Query 추가 예정:
 * <ul>
 *   <li>{@code findByPublishedAtBeforeOrderByPublishedAtDesc(LocalDateTime now, Pageable)} — 라이더 가시성 필터</li>
 *   <li>{@code findByCategoryAndPublishedAtBefore(NoticeCategory, LocalDateTime, Pageable)} — 카테고리 필터</li>
 * </ul>
 * 인덱스 1건(idx_notice_published_at)은 DDL 박제 완료.</p>
 */
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByCreatedAtDesc();
}
