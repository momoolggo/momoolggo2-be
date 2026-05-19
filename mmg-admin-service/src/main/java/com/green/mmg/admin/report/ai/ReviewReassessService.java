package com.green.mmg.admin.report.ai;

import com.green.mmg.admin.blind.entity.Blind;
import com.green.mmg.admin.blind.repository.BlindRepository;
import com.green.mmg.admin.common.enums.BlindStatus;
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

    @Async("aiTaskExecutor")
    @Transactional
    public void reassess(Long reviewId, String updatedContent) {
        log.info("AI 소명 재판정 시작 reviewId={}", reviewId);

        // blind 테이블에서 해당 리뷰 조회
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

        // AI 재판정
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
            // 블라인드 해제
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
}
