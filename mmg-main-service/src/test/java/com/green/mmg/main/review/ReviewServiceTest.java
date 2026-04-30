package com.green.mmg.main.review;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.review.model.Review;
import com.green.mmg.main.review.model.ReviewReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-C: ReviewService 단위 테스트.
 *
 * <p>학원 DB / Spring 컨텍스트 의존 0 — 순수 Mockito.
 * 권한 분기(403)는 ReviewMapper의 반환값을 mock으로 제어해 실제 분기 로직을 타게 함.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService — 단위 테스트")
class ReviewServiceTest {

    @Mock private ReviewMapper reviewMapper;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;

    private static final long USER_NO = 42L;
    private static final long OTHER_USER_NO = 99L;
    private static final long ORDER_ID = 391_000_001L;
    private static final long REVIEW_ID = 555L;
    private static final long STORE_ID = 21L;

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("postReview — 작성자 검증 + UNIQUE 충돌")
    class PostReview {

        @Test
        @DisplayName("happy: 작성자 일치 → save + storeRating 갱신 (저장 필드 동결)")
        void happyPath_savesAndUpdatesRating() {
            ReviewReq req = req(ORDER_ID, 5, "맛있어요", "img.jpg");
            when(reviewMapper.checkReviewWriter(req)).thenReturn(USER_NO);  // 일치
            when(reviewMapper.findStoreIdByOrderId(ORDER_ID)).thenReturn(STORE_ID);

            reviewService.postReview(USER_NO, req);

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).saveAndFlush(captor.capture());
            Review saved = captor.getValue();
            assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(saved.getRating()).isEqualTo(5);
            assertThat(saved.getContents()).isEqualTo("맛있어요");
            assertThat(saved.getPhoto()).isEqualTo("img.jpg");

            verify(reviewMapper).updateStoreRating(STORE_ID);
            verify(reviewMapper).findStoreIdByOrderId(ORDER_ID);
        }

        @Test
        @DisplayName("403: 다른 사용자 주문에 리뷰 시도 → FORBIDDEN '주문한 사용자가 아닙니다.' + save 미호출")
        void otherUser_throwsForbiddenAndShortCircuits() {
            ReviewReq req = req(ORDER_ID, 4, "...", null);
            when(reviewMapper.checkReviewWriter(req)).thenReturn(OTHER_USER_NO);  // 불일치

            assertThatThrownBy(() -> reviewService.postReview(USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("주문한 사용자가 아닙니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(reviewRepository, never()).saveAndFlush(any());
            verify(reviewMapper, never()).findStoreIdByOrderId(anyLong());
            verify(reviewMapper, never()).updateStoreRating(anyLong());
        }

        @Test
        @DisplayName("409: UNIQUE 위반(이미 리뷰 작성) → CONFLICT '이미 리뷰가 등록되었습니다.' + storeRating 미호출")
        void duplicateReview_throwsConflict() {
            ReviewReq req = req(ORDER_ID, 5, "두번째", null);
            when(reviewMapper.checkReviewWriter(req)).thenReturn(USER_NO);
            doThrow(new DataIntegrityViolationException("Duplicate entry"))
                    .when(reviewRepository).saveAndFlush(any(Review.class));

            assertThatThrownBy(() -> reviewService.postReview(USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("이미 리뷰가 등록되었습니다.")
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);

            verify(reviewMapper, never()).findStoreIdByOrderId(anyLong());
            verify(reviewMapper, never()).updateStoreRating(anyLong());
        }
    }

    private static ReviewReq req(long orderId, int rating, String text, String image) {
        ReviewReq req = new ReviewReq();
        req.setOrderId(orderId);
        req.setRating(rating);
        req.setText(text);
        req.setImage(image);
        return req;
    }
}
