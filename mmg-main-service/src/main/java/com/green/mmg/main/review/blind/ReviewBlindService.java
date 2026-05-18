package com.green.mmg.main.review.blind;

import com.green.mmg.main.review.ReviewRepository;
import com.green.mmg.main.review.model.Review;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewBlindService {

    private final ReviewRepository reviewRepository;

    public void blind(Long reviewId, String source, String reason, Long reportId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));
        if (review.isBlinded()) return; // 멱등성
        review.applyBlind(source, reason, reportId);
    }

    public void unblind(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));
        if (!review.isBlinded()) return;
        review.releaseBlind();
    }
}
