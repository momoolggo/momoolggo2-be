package com.green.mmg.rider.notice;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.internal.dto.RiderInternalNoticeReq;
import com.green.mmg.rider.notice.model.Notice;
import com.green.mmg.rider.notice.model.NoticeSendType;
import com.green.mmg.rider.notice.model.NoticeTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NoticeService 단위 테스트 — POST /internal/rider/notice (가짜 0건 원칙).
 *
 * <p>R3-b DeliveryServiceTest Mockito 패턴 일관 (학원 DB 의존 0).</p>
 */
@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock private NoticeRepository noticeRepository;

    @InjectMocks private NoticeService noticeService;

    @Test
    @DisplayName("NOW happy: publishedAt=now, reservedAt=null, save 호출")
    void now_happy_savesWithNowPublishedAt() {
        RiderInternalNoticeReq req = new RiderInternalNoticeReq(
                "공지 제목", NoticeTargetType.ALL, "본문", NoticeSendType.NOW, null);
        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        when(noticeRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        noticeService.createNotice(req);
        LocalDateTime after = LocalDateTime.now();

        Notice saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("공지 제목");
        assertThat(saved.getContent()).isEqualTo("본문");
        assertThat(saved.getTargetType()).isEqualTo(NoticeTargetType.ALL);
        assertThat(saved.getSendType()).isEqualTo(NoticeSendType.NOW);
        assertThat(saved.getReservedAt()).isNull();
        assertThat(saved.getPublishedAt()).isBetween(before, after);
        assertThat(saved.getSenderAdminNo()).isEqualTo(1L);
    }

    @Test
    @DisplayName("RESERVED happy: publishedAt=reservedAt, reservedAt 보존")
    void reserved_happy_savesWithReservedAt() {
        LocalDateTime future = LocalDateTime.now().plusHours(2);
        RiderInternalNoticeReq req = new RiderInternalNoticeReq(
                "예약 공지", NoticeTargetType.RIDER, "본문", NoticeSendType.RESERVED, future);
        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        when(noticeRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        noticeService.createNotice(req);

        Notice saved = captor.getValue();
        assertThat(saved.getPublishedAt()).isEqualTo(future);
        assertThat(saved.getReservedAt()).isEqualTo(future);
        assertThat(saved.getSendType()).isEqualTo(NoticeSendType.RESERVED);
        assertThat(saved.getTargetType()).isEqualTo(NoticeTargetType.RIDER);
    }

    @Test
    @DisplayName("RESERVED 과거값 → BAD_REQUEST + save 미호출")
    void reserved_pastTime_throwsBadRequest() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        RiderInternalNoticeReq req = new RiderInternalNoticeReq(
                "과거 예약", NoticeTargetType.ALL, "본문", NoticeSendType.RESERVED, past);

        assertThatThrownBy(() -> noticeService.createNotice(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("현재 시각 이후")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    @DisplayName("RESERVED reservedAt=null → BAD_REQUEST + save 미호출")
    void reserved_nullReservedAt_throwsBadRequest() {
        RiderInternalNoticeReq req = new RiderInternalNoticeReq(
                "예약 공지", NoticeTargetType.ALL, "본문", NoticeSendType.RESERVED, null);

        assertThatThrownBy(() -> noticeService.createNotice(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reservedAt이 필수")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    @DisplayName("title blank → BAD_REQUEST")
    void blankTitle_throwsBadRequest() {
        RiderInternalNoticeReq req = new RiderInternalNoticeReq(
                "  ", NoticeTargetType.ALL, "본문", NoticeSendType.NOW, null);

        assertThatThrownBy(() -> noticeService.createNotice(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("title")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("content blank → BAD_REQUEST")
    void blankContent_throwsBadRequest() {
        RiderInternalNoticeReq req = new RiderInternalNoticeReq(
                "제목", NoticeTargetType.ALL, "", NoticeSendType.NOW, null);

        assertThatThrownBy(() -> noticeService.createNotice(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("content")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("targetType=null → BAD_REQUEST")
    void nullTargetType_throwsBadRequest() {
        RiderInternalNoticeReq req = new RiderInternalNoticeReq(
                "제목", null, "본문", NoticeSendType.NOW, null);

        assertThatThrownBy(() -> noticeService.createNotice(req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("sendType=null → BAD_REQUEST")
    void nullSendType_throwsBadRequest() {
        RiderInternalNoticeReq req = new RiderInternalNoticeReq(
                "제목", NoticeTargetType.ALL, "본문", null, null);

        assertThatThrownBy(() -> noticeService.createNotice(req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("targetType=SPECIFIC → BAD_REQUEST + save 미호출 (W-2 정정, R9 도입 예정)")
    void specificTargetType_throwsBadRequest() {
        RiderInternalNoticeReq req = new RiderInternalNoticeReq(
                "제목", NoticeTargetType.SPECIFIC, "본문", NoticeSendType.NOW, null);

        assertThatThrownBy(() -> noticeService.createNotice(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SPECIFIC")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(noticeRepository, never()).save(any(Notice.class));
    }
}
