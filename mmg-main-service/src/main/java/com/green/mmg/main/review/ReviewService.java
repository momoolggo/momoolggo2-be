package com.green.mmg.main.review;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.review.model.GetReviewReq;
import com.green.mmg.main.review.model.Review;
import com.green.mmg.main.review.model.ReviewReq;
import com.green.mmg.main.review.model.ReviewRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3-C-2: postReview만 JPA 전환 + BaseEntity 첫 검증.
 *
 * <p>잔존 9 SQL (영구 MyBatis):
 * checkReviewWriter(orders 도메인), countReviews(JOIN), getReviews(복잡 JOIN+포맷+서브쿼리),
 * deleteReview(다중 DELETE JOIN), getReviewById(복잡), updateReview(다중 UPDATE JOIN),
 * findStoreIdByOrderId/findStoreIdByReviewId, updateStoreRating(cross-table)</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewRepository reviewRepository;  // postReview만

    @Transactional
    public void postReview(long user, ReviewReq req) {
        try {
            long userId = reviewMapper.checkReviewWriter(req);  // orders 도메인 SQL — 영구 잔존
            if (userId == user) {
                Review review = new Review();
                review.setOrderId(req.getOrderId());
                review.setRating(req.getRating());
                review.setContents(req.getText());
                review.setPhoto(req.getImage());
                // saveAndFlush: 후속 MyBatis findStoreIdByOrderId/updateStoreRating 가시화
                // BaseEntity Auditing — write_at/amended_at 자동 채움 검증 포인트
                reviewRepository.saveAndFlush(review);

                long storeId = reviewMapper.findStoreIdByOrderId(req.getOrderId());
                reviewMapper.updateStoreRating(storeId);
            } else {
                throw new BusinessException("주문한 사용자가 아닙니다.", HttpStatus.FORBIDDEN);
            }
        } catch (DataIntegrityViolationException e) {
            // DataIntegrityViolationException은 DuplicateKeyException의 부모 — JPA UNIQUE 위반 모두 포착
            throw new BusinessException("이미 리뷰가 등록되었습니다.", HttpStatus.CONFLICT);
        }
    }

    public Map<String, Object> getReviews(GetReviewReq req) {
        int totalCount = reviewMapper.countReviews(req.getUserNo());
        List<ReviewRes> list = reviewMapper.getReviews(req);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalCount", totalCount);
        result.put("totalPages", (int) Math.ceil((double) totalCount / req.getSize()));
        result.put("currentPage", req.getCurrentPage());
        return result;
    }

    @Transactional
    public void deleteReview(long userNo, long reviewId) {
        long storeId = reviewMapper.findStoreIdByReviewId(reviewId);
        int result = reviewMapper.deleteReview(userNo, reviewId);
        if (result == 0) throw new BusinessException("삭제 권한이 없습니다.", HttpStatus.FORBIDDEN);
        reviewMapper.updateStoreRating(storeId);
    }

    public Map<String, Object> getReviewById(long reviewId) {
        Map<String, Object> review = reviewMapper.getReviewById(reviewId);
        if (review == null) throw new BusinessException("리뷰를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        return review;
    }

    @Transactional
    public void updateReview(long userNo, long reviewId, int rating, String contents) {
        int result = reviewMapper.updateReview(reviewId, userNo, rating, contents);
        if (result == 0) throw new BusinessException("수정 권한이 없거나 리뷰를 찾을 수 없습니다.", HttpStatus.FORBIDDEN);
        long storeId = reviewMapper.findStoreIdByReviewId(reviewId);
        reviewMapper.updateStoreRating(storeId);
    }
}
