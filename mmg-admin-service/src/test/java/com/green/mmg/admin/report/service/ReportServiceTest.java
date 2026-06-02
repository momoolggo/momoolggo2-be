package com.green.mmg.admin.report.service;

import com.green.mmg.admin.report.dto.ReportReq;
import com.green.mmg.admin.report.entity.Report;
import com.green.mmg.admin.report.event.ReviewReportSubmittedEvent;
import com.green.mmg.admin.report.repository.ReportRepository;
import com.green.mmg.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("same reporter cannot report the same review twice")
    void duplicateReport_existingRow_throwsConflict() {
        ReportReq req = new ReportReq(10L, 20L, "reason", "content", "review");
        when(reportRepository.existsByReporterNoAndTargetTypeAndTargetNo(20L, "리뷰", 10L)).thenReturn(true);

        assertThatThrownBy(() -> reportService.reportReview(req))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(reportRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("report save publishes AI event and keeps review content")
    void reportReview_success_publishesEvent() {
        ReportReq req = new ReportReq(10L, 20L, "reason", "content", "review body");
        when(reportRepository.existsByReporterNoAndTargetTypeAndTargetNo(20L, "리뷰", 10L)).thenReturn(false);
        when(reportRepository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "reportId", 30L);
            return report;
        });

        Long reportId = reportService.reportReview(req);

        assertThat(reportId).isEqualTo(30L);

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getReporterNo()).isEqualTo(20L);
        assertThat(reportCaptor.getValue().getTargetNo()).isEqualTo(10L);
        assertThat(reportCaptor.getValue().getReviewContent()).isEqualTo("review body");

        ArgumentCaptor<ReviewReportSubmittedEvent> eventCaptor =
                ArgumentCaptor.forClass(ReviewReportSubmittedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().reportId()).isEqualTo(30L);
    }

    @Test
    @DisplayName("database unique violation is converted to conflict")
    void duplicateReport_uniqueRace_throwsConflict() {
        ReportReq req = new ReportReq(10L, 20L, "reason", "content", "review");
        when(reportRepository.existsByReporterNoAndTargetTypeAndTargetNo(20L, "리뷰", 10L)).thenReturn(false);
        when(reportRepository.saveAndFlush(any(Report.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> reportService.reportReview(req))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
