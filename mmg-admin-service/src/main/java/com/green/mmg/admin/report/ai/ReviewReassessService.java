package com.green.mmg.admin.report.ai;

import com.green.mmg.admin.blind.entity.Blind;
import com.green.mmg.admin.blind.repository.BlindRepository;
import com.green.mmg.admin.common.enums.BlindReason;
import com.green.mmg.admin.common.enums.BlindStatus;
import com.green.mmg.admin.dto.feign.InternalReviewRes;
import com.green.mmg.admin.feign.MainFeignClient;
import com.green.mmg.admin.report.dto.AiReviewJudgement;
import com.green.mmg.admin.report.feign.ReviewBlindClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewReassessService {

    private final GeminiReviewClassifier classifier;
    private final ReviewBlindClient reviewBlindClient;
    private final BlindRepository blindRepository;
    private final MainFeignClient mainFeignClient;

    // 소명 재판정 (블라인드된 리뷰 수정 시)
    @Async("aiTaskExecutor")
    @Transactional
    public void reassess(Long reviewId, String updatedContent) {
        log.info("AI 소명 재판정 시작 reviewId={}", reviewId);

        List<Blind> blinds = blindRepository.findByReviewNo(reviewId);
        if (blinds.isEmpty()) {
            log.warn("블라인드 정보 없음 reviewId={}", reviewId);
            return;
        }
        Blind blind = blinds.stream()
                .filter(b -> b.getStatus() == BlindStatus.BLINDED)
                .findFirst()
                .orElse(null);
        if (blind == null) {
            log.info("이미 해제된 블라인드 reviewId={}", reviewId);
            return;
        }

        AiReviewJudgement judgement;
        try {
            judgement = classifier.judge(updatedContent, "소명 재판정");
        } catch (Exception e) {
            log.error("AI 재판정 실패 reviewId={} error={}", reviewId, e.getMessage());
            return;
        }

        log.info("AI 재판정 결과 reviewId={} shouldBlind={} confidence={}",
                reviewId, judgement.shouldBlind(), judgement.confidence());

        if (!judgement.shouldBlind()) {
            try {
                reviewBlindClient.unblind(reviewId, new ReviewBlindClient.UnblindRequest(null, "소명 완료 - AI 재판정 통과"));
                blind.release();
                log.info("소명 완료 - 블라인드 해제 reviewId={}", reviewId);
            } catch (Exception e) {
                log.error("블라인드 해제 실패 reviewId={} error={}", reviewId, e.getMessage());
            }
        } else {
            log.info("소명 거부 - 블라인드 유지 reviewId={} reason={}", reviewId, judgement.reason());
        }
    }

    // 자동 감지 (새 리뷰 작성 시)
    @Async("aiTaskExecutor")
    @Transactional
    public void autoDetect(Long reviewId, String content) {
        log.info("AI 자동 감지 시작 reviewId={}", reviewId);

        AiReviewJudgement judgement;
        try {
            judgement = classifier.judge(content, "자동 감지");
        } catch (Exception e) {
            log.error("AI 자동 감지 실패 reviewId={} error={}", reviewId, e.getMessage());
            return;
        }

        log.info("AI 자동 감지 결과 reviewId={} shouldBlind={} confidence={}",
                reviewId, judgement.shouldBlind(), judgement.confidence());

        boolean gate = judgement.shouldBlind() && !"LOW".equals(judgement.confidence());
        if (gate) {
            try {
                reviewBlindClient.blind(reviewId,
                        new ReviewBlindClient.BlindRequest("AUTO_AI", judgement.reason(), null));

                // 리뷰 정보 조회
                String storeName = null;
                String writer = null;
                Double rating = null;
                Long userNo = 0L;
                try {
                    InternalReviewRes reviewInfo = mainFeignClient.getReviewById(reviewId).getResultData();
                    if (reviewInfo != null) {
                        storeName = reviewInfo.getStoreName();
                        writer = reviewInfo.getWriter();
                        rating = reviewInfo.getRating();
                        userNo = reviewInfo.getUserNo() != null ? reviewInfo.getUserNo() : 0L;
                    }
                } catch (Exception e) {
                    log.warn("리뷰 정보 조회 실패: {}", e.getMessage());
                }

                BlindReason blindReason = parseBlindReason(judgement.violations());
                Blind blind = new Blind(reviewId, userNo, blindReason, storeName, content, rating, writer);
                blindRepository.save(blind);
                log.info("자동 감지 블라인드 완료 reviewId={}", reviewId);
            } catch (Exception e) {
                log.error("자동 감지 블라인드 처리 실패 reviewId={} error={}", reviewId, e.getMessage());
            }
        }
    }

    private BlindReason parseBlindReason(List<String> violations) {
        if (violations == null || violations.isEmpty()) return BlindReason.ETC;
        String v = violations.get(0);
        if (v.contains("욕설") || v.contains("혐오")) return BlindReason.PROFANITY;
        if (v.contains("광고")) return BlindReason.ADVERTISEMENT;
        if (v.contains("허위")) return BlindReason.FALSE_FACT;
        return BlindReason.ETC;
    }
}
