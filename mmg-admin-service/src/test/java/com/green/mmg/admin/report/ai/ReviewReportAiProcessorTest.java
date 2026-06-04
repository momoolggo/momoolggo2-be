package com.green.mmg.admin.report.ai;

import com.green.mmg.admin.blind.entity.Blind;
import com.green.mmg.admin.blind.repository.BlindRepository;
import com.green.mmg.admin.common.enums.BlindStatus;
import com.green.mmg.admin.feign.MainFeignClient;
import com.green.mmg.admin.report.dto.AiReviewJudgement;
import com.green.mmg.admin.report.entity.Report;
import com.green.mmg.admin.report.feign.ReviewBlindClient;
import com.green.mmg.admin.report.repository.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewReportAiProcessorTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private GeminiReviewClassifier classifier;

    @Mock
    private ReviewBlindClient reviewBlindClient;

    @Mock
    private BlindRepository blindRepository;

    @Mock
    private MainFeignClient mainFeignClient;

    @InjectMocks
    private ReviewReportAiProcessService processor;

    @Test
    @DisplayName("active blind suppresses duplicate blind creation")
    void process_activeBlindExists_skipsBlindCreation() {
        Report report = new Report(20L, 10L, "reason", "content");
        report.setReviewContent("review body");
        ReflectionTestUtils.setField(report, "reportId", 30L);

        when(reportRepository.findById(30L)).thenReturn(Optional.of(report));
        when(classifier.judge("review body", "reason"))
                .thenReturn(new AiReviewJudgement(true, "HIGH", "bad", List.of("bad")));
        when(reportRepository.findFirstByTargetNoOrderByReportIdAsc(10L))
                .thenReturn(Optional.of(report));
        when(blindRepository.existsByReviewNoAndStatusIn(
                eq(10L),
                eq(List.of(BlindStatus.REVIEWING, BlindStatus.BLINDED, BlindStatus.SUSPENDED, BlindStatus.PERMANENT))
        )).thenReturn(true);

        processor.process(30L);

        verify(reviewBlindClient, never()).blind(any(), any());
        verify(blindRepository, never()).save(any(Blind.class));
        verify(mainFeignClient, never()).getReviewById(any());
        verify(reportRepository).save(report);
    }
}
