package com.green.mmg.admin.report.ai;

import com.green.mmg.admin.report.dto.AiReviewJudgement;
import com.green.mmg.admin.report.entity.AiStatus;
import com.green.mmg.admin.report.entity.Report;
import com.green.mmg.admin.report.event.ReviewReportSubmittedEvent;
import com.green.mmg.admin.report.feign.ReviewBlindClient;
import com.green.mmg.admin.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewReportAiProcessor {

    private final ReportRepository reportRepository;
    private final GeminiReviewClassifier classifier;
    private final ReviewBlindClient reviewBlindClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("aiTaskExecutor")
    public void onReportSubmitted(ReviewReportSubmittedEvent event) {
        process(event.reportId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long reportId) {
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            log.warn("AI 처리 대상 신고 없음 reportId={}", reportId);
            return;
        }
        if (report.getAiStatus() == AiStatus.DONE) {
            log.info("이미 AI 처리 완료 reportId={}", reportId);
            return;
        }

        log.info("AI 리뷰 심사 시작 reportId={}", reportId);

        AiReviewJudgement judgement;
        try {
            String reviewContent = report.getReviewContent() != null
                    ? report.getReviewContent() : "(내용 없음)";
            judgement = classifier.judge(reviewContent, report.getReason());
        } catch (Exception e) {
            log.error("AI 판정 실패 reportId={} error={}", reportId, e.getMessage());
            report.markAiFailed(safeMsg(e));
            reportRepository.save(report);
            return;
        }

        String violationsStr = judgement.violations() != null
                ? String.join(",", judgement.violations()) : "";
        report.applyAiResult(judgement.shouldBlind(), judgement.reason(),
                violationsStr, judgement.confidence());

        // 자동 블라인드 게이트: shouldBlind=true && confidence != LOW
        boolean gate = judgement.shouldBlind() && !"LOW".equals(judgement.confidence());
        if (gate) {
            try {
                reviewBlindClient.blind(
                        report.getTargetNo(),
                        new ReviewBlindClient.BlindRequest("AUTO_AI", judgement.reason(), reportId)
                );
                report.markAutoBlinded();
                log.info("자동 블라인드 완료 reportId={} reviewId={}", reportId, report.getTargetNo());
            } catch (Exception e) {
                log.error("블라인드 Feign 실패 reportId={} error={}", reportId, e.getMessage());
                report.markBlindFailed(safeMsg(e));
            }
        } else {
            log.info("블라인드 게이트 미통과 reportId={} shouldBlind={} confidence={}",
                    reportId, judgement.shouldBlind(), judgement.confidence());
        }

        reportRepository.save(report);
    }

    private String safeMsg(Exception e) {
        String msg = e.getMessage();
        return msg != null ? msg.substring(0, Math.min(490, msg.length())) : "unknown";
    }
}
