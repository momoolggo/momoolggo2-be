package com.green.mmg.main.review;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.review.model.GetReviewReq;
import com.green.mmg.main.review.model.Review;
import com.green.mmg.main.review.model.ReviewReq;
import com.green.mmg.main.review.model.ReviewRes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getReviews — 페이징 응답 조립")
    class GetReviews {

        @Test
        @DisplayName("happy: list + totalCount + totalPages(올림) + currentPage 모두 동결")
        void happyPath_assemblesPagingResponse() {
            GetReviewReq req = new GetReviewReq();
            req.setUserNo(USER_NO);
            req.setCurrentPage(2);
            req.setSize(5);

            ReviewRes r1 = new ReviewRes();
            r1.setReviewId(101L);
            ReviewRes r2 = new ReviewRes();
            r2.setReviewId(102L);
            when(reviewMapper.countReviews(USER_NO)).thenReturn(7);   // totalPages = ceil(7/5) = 2
            when(reviewMapper.getReviews(req)).thenReturn(List.of(r1, r2));

            Map<String, Object> result = reviewService.getReviews(req);

            assertThat(result.get("totalCount")).isEqualTo(7);
            assertThat(result.get("totalPages")).isEqualTo(2);
            assertThat(result.get("currentPage")).isEqualTo(2);
            @SuppressWarnings("unchecked")
            List<ReviewRes> list = (List<ReviewRes>) result.get("list");
            assertThat(list).hasSize(2);
            assertThat(list.get(0).getReviewId()).isEqualTo(101L);
            assertThat(list.get(1).getReviewId()).isEqualTo(102L);

            verify(reviewMapper).countReviews(USER_NO);
            verify(reviewMapper).getReviews(req);
            verifyNoMoreInteractions(reviewMapper);
            verifyNoInteractions(reviewRepository);
        }

        @Test
        @DisplayName("count=0 → totalPages=0 + 빈 list (NPE 없음)")
        void zeroCount_returnsZeroPages() {
            GetReviewReq req = new GetReviewReq();
            req.setUserNo(USER_NO);
            when(reviewMapper.countReviews(USER_NO)).thenReturn(0);
            when(reviewMapper.getReviews(req)).thenReturn(List.of());

            Map<String, Object> result = reviewService.getReviews(req);

            assertThat(result.get("totalCount")).isEqualTo(0);
            assertThat(result.get("totalPages")).isEqualTo(0);
            assertThat(((List<?>) result.get("list"))).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getReviewById — 단건 조회")
    class GetReviewById {

        @Test
        @DisplayName("happy: Mapper Map 그대로 반환")
        void happyPath_returnsMap() {
            Map<String, Object> raw = new HashMap<>();
            raw.put("reviewId", REVIEW_ID);
            raw.put("rating", 4);
            raw.put("contents", "괜찮음");
            when(reviewMapper.getReviewById(REVIEW_ID)).thenReturn(raw);

            Map<String, Object> result = reviewService.getReviewById(REVIEW_ID);

            assertThat(result).isSameAs(raw);
            assertThat(result.get("reviewId")).isEqualTo(REVIEW_ID);
            assertThat(result.get("rating")).isEqualTo(4);
            assertThat(result.get("contents")).isEqualTo("괜찮음");
            verify(reviewMapper).getReviewById(REVIEW_ID);
            verifyNoMoreInteractions(reviewMapper);
        }

        @Test
        @DisplayName("404: null → BusinessException NOT_FOUND '리뷰를 찾을 수 없습니다.'")
        void notFound_throwsNotFound() {
            when(reviewMapper.getReviewById(REVIEW_ID)).thenReturn(null);

            assertThatThrownBy(() -> reviewService.getReviewById(REVIEW_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("리뷰를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteReview — 삭제 권한 + storeRating 갱신")
    class DeleteReview {

        @Test
        @DisplayName("happy: result>0 → updateStoreRating 호출 (호출 순서 동결: findStoreId → delete → updateRating)")
        void happyPath_deletesAndUpdatesRating() {
            when(reviewMapper.findStoreIdByReviewId(REVIEW_ID)).thenReturn(STORE_ID);
            when(reviewMapper.deleteReview(USER_NO, REVIEW_ID)).thenReturn(1);

            reviewService.deleteReview(USER_NO, REVIEW_ID);

            InOrder inOrder = inOrder(reviewMapper);
            inOrder.verify(reviewMapper).findStoreIdByReviewId(REVIEW_ID);
            inOrder.verify(reviewMapper).deleteReview(USER_NO, REVIEW_ID);
            inOrder.verify(reviewMapper).updateStoreRating(STORE_ID);
            verifyNoInteractions(reviewRepository);
        }

        @Test
        @DisplayName("403: result==0 (다른 사용자 리뷰) → FORBIDDEN '삭제 권한이 없습니다.' + updateStoreRating 미호출")
        void noPermission_throwsForbiddenAndShortCircuits() {
            when(reviewMapper.findStoreIdByReviewId(REVIEW_ID)).thenReturn(STORE_ID);
            when(reviewMapper.deleteReview(OTHER_USER_NO, REVIEW_ID)).thenReturn(0);

            assertThatThrownBy(() -> reviewService.deleteReview(OTHER_USER_NO, REVIEW_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("삭제 권한이 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(reviewMapper, never()).updateStoreRating(anyLong());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateReview — 수정 권한 + storeRating 갱신")
    class UpdateReview {

        @Test
        @DisplayName("happy: result>0 → updateStoreRating 호출 (호출 순서 동결: update → findStoreId → updateRating)")
        void happyPath_updatesAndRefreshesRating() {
            when(reviewMapper.updateReview(REVIEW_ID, USER_NO, 3, "수정")).thenReturn(1);
            when(reviewMapper.findStoreIdByReviewId(REVIEW_ID)).thenReturn(STORE_ID);

            reviewService.updateReview(USER_NO, REVIEW_ID, 3, "수정");

            InOrder inOrder = inOrder(reviewMapper);
            inOrder.verify(reviewMapper).updateReview(REVIEW_ID, USER_NO, 3, "수정");
            inOrder.verify(reviewMapper).findStoreIdByReviewId(REVIEW_ID);
            inOrder.verify(reviewMapper).updateStoreRating(STORE_ID);
            verifyNoInteractions(reviewRepository);
        }

        @Test
        @DisplayName("403: result==0 → FORBIDDEN '수정 권한이 없거나 리뷰를 찾을 수 없습니다.' + findStoreId/updateRating 미호출")
        void noPermission_throwsForbiddenAndShortCircuits() {
            when(reviewMapper.updateReview(REVIEW_ID, OTHER_USER_NO, 1, "x")).thenReturn(0);

            assertThatThrownBy(() -> reviewService.updateReview(OTHER_USER_NO, REVIEW_ID, 1, "x"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("수정 권한이 없거나 리뷰를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(reviewMapper, never()).findStoreIdByReviewId(anyLong());
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
