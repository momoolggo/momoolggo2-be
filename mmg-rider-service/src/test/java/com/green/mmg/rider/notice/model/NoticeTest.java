package com.green.mmg.rider.notice.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Notice entity 단위 테스트 (JPA / Repository / DB 의존 0).
 *
 * <p>R2-a Delivery / R2-c WorkSession / R2-d Settlement 패턴 일관 — Mockito Mock 기반.
 * R9 NoticeService 진입 시 통합 테스트 추가.</p>
 *
 * <p>검증 본질: 명시 생성자 결과 (필수 5필드 매핑 + category enum + published_at 호출자 제어).</p>
 */
@DisplayName("Notice entity 단위")
class NoticeTest {

    @Test
    @DisplayName("생성자: 필수 8필드 매핑 + category IMPORTANT 박제")
    void constructor_setsRequiredFields() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 5, 7, 9, 0, 0);

        Notice notice = new Notice(
                NoticeCategory.IMPORTANT,
                "5월 안전 운전 캠페인",
                "5월 한 달간 헬멧 착용 필수입니다.",
                NoticeTargetType.ALL,
                NoticeSendType.NOW,
                null,
                publishedAt,
                1L
        );

        assertThat(notice.getCategory()).isEqualTo(NoticeCategory.IMPORTANT);
        assertThat(notice.getTitle()).isEqualTo("5월 안전 운전 캠페인");
        assertThat(notice.getContent()).isEqualTo("5월 한 달간 헬멧 착용 필수입니다.");
        assertThat(notice.getTargetType()).isEqualTo(NoticeTargetType.ALL);
        assertThat(notice.getSendType()).isEqualTo(NoticeSendType.NOW);
        assertThat(notice.getReservedAt()).isNull();
        assertThat(notice.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(notice.getSenderAdminNo()).isEqualTo(1L);
        assertThat(notice.getNoticeNo()).isNull();
    }

    @Test
    @DisplayName("생성자: 카테고리 화이트리스트 3종(IMPORTANT/SAFETY/GENERAL) snapshot 보존")
    void constructor_categoryWhitelist_preserved() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 5, 7, 12, 0, 0);

        Notice important = new Notice(NoticeCategory.IMPORTANT, "T1", "C1",
                NoticeTargetType.ALL, NoticeSendType.NOW, null, publishedAt, 1L);
        Notice safety = new Notice(NoticeCategory.SAFETY, "T2", "C2",
                NoticeTargetType.ALL, NoticeSendType.NOW, null, publishedAt, 1L);
        Notice general = new Notice(NoticeCategory.GENERAL, "T3", "C3",
                NoticeTargetType.ALL, NoticeSendType.NOW, null, publishedAt, 1L);

        assertThat(important.getCategory()).isEqualTo(NoticeCategory.IMPORTANT);
        assertThat(safety.getCategory()).isEqualTo(NoticeCategory.SAFETY);
        assertThat(general.getCategory()).isEqualTo(NoticeCategory.GENERAL);
    }

    @Test
    @DisplayName("생성자: published_at 호출자 제어 (즉시 NOW / 예약 미래 / 과거 시각도 가능)")
    void constructor_publishedAt_callerControlled() {
        LocalDateTime past = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime future = LocalDateTime.of(2027, 6, 15, 10, 30, 0);

        Notice notice1 = new Notice(NoticeCategory.GENERAL, "Past", "C",
                NoticeTargetType.ALL, NoticeSendType.NOW, null, past, 1L);
        Notice notice2 = new Notice(NoticeCategory.GENERAL, "Future", "C",
                NoticeTargetType.ALL, NoticeSendType.RESERVED, future, future, 1L);

        assertThat(notice1.getPublishedAt()).isEqualTo(past);
        assertThat(notice2.getPublishedAt()).isEqualTo(future);
        assertThat(notice2.getReservedAt()).isEqualTo(future);
        assertThat(notice2.getSendType()).isEqualTo(NoticeSendType.RESERVED);
    }
}
