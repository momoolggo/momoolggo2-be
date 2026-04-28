package com.green.mmg.main.review;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.review.model.GetReviewReq;
import com.green.mmg.main.review.model.ReviewReq;
import com.green.mmg.main.review.model.ReviewRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;

    // 리뷰 등록 + 가게 별점 자동 갱신
    @Transactional
    public void postReview(long user, ReviewReq req) {
        try {
            long userId = reviewMapper.checkReviewWriter(req);
            if (userId == user) {
                reviewMapper.postReview(req);
                long storeId = reviewMapper.findStoreIdByOrderId(req.getOrderId());
                reviewMapper.updateStoreRating(storeId);
            } else {
                throw new BusinessException("주문한 사용자가 아닙니다.", HttpStatus.FORBIDDEN);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException("이미 리뷰가 등록되었습니다.", HttpStatus.CONFLICT);
        }
    }

    // 리뷰 목록 조회 (페이지네이션)
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

    // 리뷰 삭제 + 가게 별점 자동 갱신
    @Transactional
    public void deleteReview(long userNo, long reviewId) {
        long storeId = reviewMapper.findStoreIdByReviewId(reviewId);
        int result = reviewMapper.deleteReview(userNo, reviewId);
        if (result == 0) throw new BusinessException("삭제 권한이 없습니다.", HttpStatus.FORBIDDEN);
        reviewMapper.updateStoreRating(storeId);
    }

    // 리뷰 단건 조회 (수정 시 기존 데이터 불러오기)
    public Map<String, Object> getReviewById(long reviewId) {
        Map<String, Object> review = reviewMapper.getReviewById(reviewId);
        if (review == null) throw new BusinessException("리뷰를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        return review;
    }

    // 리뷰 수정 + 가게 별점 자동 갱신
    @Transactional
    public void updateReview(long userNo, long reviewId, int rating, String contents) {
        int result = reviewMapper.updateReview(reviewId, userNo, rating, contents);
        if (result == 0) throw new BusinessException("수정 권한이 없거나 리뷰를 찾을 수 없습니다.", HttpStatus.FORBIDDEN);
        long storeId = reviewMapper.findStoreIdByReviewId(reviewId);
        reviewMapper.updateStoreRating(storeId);
    }
}
