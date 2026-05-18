package com.green.mmg.admin.report.ai;

import com.green.mmg.admin.report.entity.AiStatus;
import com.green.mmg.admin.report.entity.Report;
import com.green.mmg.admin.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewReportAiRetryScheduler {

    private final ReportRepository reportRepository;
    private final ReviewReportAiProcessor aiProcessor;

    // 10분마다 실패한 AI 처리 재시도 (최대 3회)
    @Scheduled(fixedDelay = 600_000)
    public void retryFailed() {
        List<Report> failed = reportRepository
                .findByAiStatusAndAiRetryCountLessThan(AiStatus.FAILED, 3);
        if (failed.isEmpty()) return;
        log.info("AI 재시도 대상 {}건", failed.size());
        failed.forEach(r -> {
            try {
                aiProcessor.process(r.getReportId());
            } catch (Exception e) {
                log.error("AI 재시도 실패 reportId={} error={}", r.getReportId(), e.getMessage());
            }
        });
    }
}
