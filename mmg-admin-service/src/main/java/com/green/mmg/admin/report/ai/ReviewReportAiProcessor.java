package com.green.mmg.admin.report.ai;

import com.green.mmg.admin.blind.entity.Blind;
import com.green.mmg.admin.blind.repository.BlindRepository;
import com.green.mmg.admin.common.enums.BlindReason;
import com.green.mmg.admin.dto.feign.InternalReviewRes;
import com.green.mmg.admin.dto.feign.InternalStoreListPageRes;
import com.green.mmg.admin.dto.feign.InternalStoreListRes;
import com.green.mmg.admin.dto.feign.NotificationCreateReq;
import com.green.mmg.admin.feign.MainFeignClient;
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


import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewReportAiProcessor {

    private final ReportRepository reportRepository;
    private final GeminiReviewClassifier classifier;
    private final ReviewBlindClient reviewBlindClient;
    private final BlindRepository blindRepository;
    private final MainFeignClient mainFeignClient;

    private Map<Long, String> getStoreNameMap() {
        try {
            InternalStoreListPageRes res = mainFeignClient
                    .getStoreList(0, 200, null, null, null, null, null)
                    .getResultData();
            if (res == null || res.getContent() == null) return Map.of();
            return res.getContent().stream()
                    .collect(Collectors.toMap(InternalStoreListRes::getStoreId, InternalStoreListRes::getStoreName));
        } catch (Exception e) {
            return Map.of();
        }
    }

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
                // 1. main-service review 블라인드 처리
                reviewBlindClient.blind(
                        report.getTargetNo(),
                        new ReviewBlindClient.BlindRequest("AUTO_AI", judgement.reason(), reportId)
                );
                report.markAutoBlinded();

                // 2. blind 테이블 INSERT (AdminBlindView 탭에 표시)
                BlindReason blindReason = parseBlindReason(report.getReason());

                // 리뷰 정보 조회 (storeName, writer, rating, 작성자 userNo)
                String storeName = null;
                String writer = null;
                Double rating = null;
                Long reviewUserNo = report.getReporterNo(); // blind 엔티티 non-null용 기본값
                boolean authorResolved = false;
                try {
                    InternalReviewRes reviewInfo = mainFeignClient.getReviewById(report.getTargetNo()).getResultData();
                    if (reviewInfo != null) {
                        storeName = reviewInfo.getStoreName();
                        writer = reviewInfo.getWriter();
                        rating = reviewInfo.getRating();
                        if (reviewInfo.getUserNo() != null) {
                            reviewUserNo = reviewInfo.getUserNo(); // 실제 리뷰 작성자
                            authorResolved = true;
                        }
                    }
                } catch (Exception e) {
                    log.warn("리뷰 정보 조회 실패: {}", e.getMessage());
                }

                Blind blind = new Blind(
                        report.getTargetNo(),      // reviewNo
                        reviewUserNo,              // userNo (리뷰 작성자)
                        blindReason,
                        storeName,
                        report.getReviewContent(),
                        rating,
                        writer
                );
                blindRepository.save(blind);

                log.info("자동 블라인드 완료 reportId={} reviewId={}", reportId, report.getTargetNo());

                // 3. 리뷰 작성자에게 소명 안내 알림 발송 (작성자 확인된 경우만)
                if (authorResolved) {
                    try {
                        NotificationCreateReq notiReq = NotificationCreateReq.reviewBlind(
                                reviewUserNo, blind.getBlindId(), judgement.reason()
                        );
                        mainFeignClient.createNotification(notiReq);
                        log.info("소명 안내 알림 발송 완료 userNo={} blindId={}", reviewUserNo, blind.getBlindId());
                    } catch (Exception e) {
                        log.warn("소명 안내 알림 발송 실패 userNo={}: {}", reviewUserNo, e.getMessage());
                    }
                } else {
                    log.warn("리뷰 작성자 미확인으로 알림 미발송 reviewId={}", report.getTargetNo());
                }
            } catch (Exception e) {
                log.error("블라인드 처리 실패 reportId={} error={}", reportId, e.getMessage());
                report.markBlindFailed(safeMsg(e));
            }
        } else {
            log.info("블라인드 게이트 미통과 reportId={} shouldBlind={} confidence={}",
                    reportId, judgement.shouldBlind(), judgement.confidence());
        }

        reportRepository.save(report);
    }

    // 신고 사유 → BlindReason 매핑
    private BlindReason parseBlindReason(String reason) {
        if (reason == null) return BlindReason.ETC;
        if (reason.contains("욕설") || reason.contains("혐오")) return BlindReason.PROFANITY;
        if (reason.contains("광고")) return BlindReason.ADVERTISEMENT;
        if (reason.contains("허위")) return BlindReason.FALSE_FACT;
        return BlindReason.ETC;
    }

    private String safeMsg(Exception e) {
        String msg = e.getMessage();
        return msg != null ? msg.substring(0, Math.min(490, msg.length())) : "unknown";
    }
}
