package com.green.mmg.main.review.blind;

import com.green.mmg.main.notification.NotificationService;
import com.green.mmg.main.notification.model.NotificationCreateReq;
import com.green.mmg.main.order.OrderRepository;
import com.green.mmg.main.order.model.Orders;
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
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public void blind(Long reviewId, String source, String reason, Long reportId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));
        if (review.isBlinded()) return; // 멱등성

        review.applyBlind(source, reason, reportId);
        sendReviewBlindNotification(review);
    }

    public void unblind(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));
        if (!review.isBlinded()) return;

        review.releaseBlind();
    }

    private void sendReviewBlindNotification(Review review) {
        Orders order = orderRepository.findById(review.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + review.getOrderId()));

        notificationService.createNotification(new NotificationCreateReq(
                order.getUserNo(),
                "REVIEW_BLIND_NOTICE",
                "작성한 리뷰가 블라인드 처리되었습니다.",
                "7일 이내 리뷰를 수정하면 소명 완료 처리됩니다.",
                "/mypage/reviews/" + review.getReviewId() + "/edit"
        ));
    }
}