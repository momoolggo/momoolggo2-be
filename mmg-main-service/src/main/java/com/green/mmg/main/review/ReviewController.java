package com.green.mmg.main.review;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.review.model.GetReviewReq;
import com.green.mmg.main.review.model.ReviewReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 리뷰 도메인. 경로는 `/api/user/review/**` 유지 (api-spec 동결).
 * 원본 monolith의 UserController에 섞여있던 리뷰 5개 endpoint를 main-service로 분리.
 */
@Slf4j
@RestController
@RequestMapping("/api/user/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public void postReview(@AuthenticationPrincipal UserPrincipal userPrincipal,
                           @RequestBody ReviewReq req) {
        reviewService.postReview(userPrincipal.getSignedUserNo(), req);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute GetReviewReq req) {
        req.setUserNo(principal.getSignedUserNo());
        return ResponseEntity.ok(reviewService.getReviews(req));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable long reviewId) {
        reviewService.deleteReview(principal.getSignedUserNo(), reviewId);
        return ResponseEntity.ok(Map.of("result", "삭제성공"));
    }

    // 리뷰 단건 조회 (수정 시 기존 데이터 불러오기)
    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReviewById(@PathVariable long reviewId) {
        return ResponseEntity.ok(reviewService.getReviewById(reviewId));
    }

    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable long reviewId,
            @RequestBody Map<String, Object> body) {
        int rating = Integer.parseInt(body.get("rating").toString());
        String contents = body.get("contents").toString();
        reviewService.updateReview(principal.getSignedUserNo(), reviewId, rating, contents);
        return ResponseEntity.ok(Map.of("result", "수정성공"));
    }
}
