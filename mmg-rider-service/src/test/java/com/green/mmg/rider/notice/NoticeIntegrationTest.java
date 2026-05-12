package com.green.mmg.rider.notice;

import com.green.mmg.rider.notice.dto.RiderNoticeRowRes;
import com.green.mmg.rider.notice.model.Notice;
import com.green.mmg.rider.notice.model.NoticeCategory;
import com.green.mmg.rider.notice.model.NoticeSendType;
import com.green.mmg.rider.notice.model.NoticeTargetType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 5-R9: NoticeService 통합 테스트 — 실 학원 DB + JPA 가시성 필터 검증.
 *
 * <p>R3-c DeliveryServiceIntegrationTest 정착 패턴 일관 ({@code @SpringBootTest + @Transactional + @Rollback +
 * fixture INSERT}, {@code feedback_integration_test_setup.md}).</p>
 *
 * <p>1건: target_type IN (ALL, RIDER) + published_at <= now 필터 + SPECIFIC 제외 + 미래 RESERVED 제외 검증.</p>
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("NoticeService 통합 (실 학원 DB)")
class NoticeIntegrationTest {

    @Autowired private NoticeService noticeService;
    @Autowired private NoticeRepository noticeRepository;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("R9 라이더 가시성 — ALL/RIDER + published<=now 통과, SPECIFIC + 미래 published 제외")
    void riderVisibility_filtersTargetAndFutureReserved() {
        LocalDateTime now = LocalDateTime.now();
        // 보이는 행 3건
        Notice allVisible = noticeRepository.saveAndFlush(new Notice(
                NoticeCategory.IMPORTANT, "안전 공지", "안전 본문",
                NoticeTargetType.ALL, NoticeSendType.NOW,
                null, now.minusMinutes(10), 1L));
        Notice riderVisible = noticeRepository.saveAndFlush(new Notice(
                NoticeCategory.SAFETY, "라이더 전용", "라이더 본문",
                NoticeTargetType.RIDER, NoticeSendType.NOW,
                null, now.minusMinutes(5), 1L));
        // 안 보이는 행 2건
        Notice specificHidden = noticeRepository.saveAndFlush(new Notice(
                NoticeCategory.GENERAL, "특정 대상", "본문",
                NoticeTargetType.SPECIFIC, NoticeSendType.NOW,
                null, now.minusMinutes(1), 1L));  // SPECIFIC 제외
        Notice futureHidden = noticeRepository.saveAndFlush(new Notice(
                NoticeCategory.GENERAL, "예약 발송", "본문",
                NoticeTargetType.ALL, NoticeSendType.RESERVED,
                now.plusHours(1), now.plusHours(1), 1L));  // 미래 published 제외
        em.flush();
        em.clear();

        List<RiderNoticeRowRes> result = noticeService.getRiderNoticeList();

        assertThat(result).extracting(RiderNoticeRowRes::noticeNo)
                .contains(allVisible.getNoticeNo(), riderVisible.getNoticeNo())
                .doesNotContain(specificHidden.getNoticeNo(), futureHidden.getNoticeNo());
        // DESC 정렬: riderVisible(-5분) > allVisible(-10분)
        int allIdx = -1, riderIdx = -1;
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).noticeNo().equals(allVisible.getNoticeNo())) allIdx = i;
            if (result.get(i).noticeNo().equals(riderVisible.getNoticeNo())) riderIdx = i;
        }
        assertThat(riderIdx).isLessThan(allIdx);  // riderVisible이 더 앞 (DESC)
    }
}
