package com.green.mmg.main.review;

import com.green.mmg.main.order.OrderRepository;
import com.green.mmg.main.order.model.Orders;
import com.green.mmg.main.review.model.Review;
import com.green.mmg.main.review.model.ReviewReq;
import com.green.mmg.main.support.SnapshotAssert;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Phase 3-C-2 검증: Review postReview JPA 전환 + **BaseEntity 첫 검증**.
 *
 * <p>핵심 검증 포인트:
 * <ol>
 *   <li>{@code @CreatedDate}가 INSERT 시 write_at 자동 채움</li>
 *   <li>{@code @LastModifiedDate}가 INSERT 시 amended_at 자동 채움 (UPDATE는 MyBatis updateReview에서
 *       NOW() 명시 — JPA dirty checking 안 거침, BaseEntity와 무관)</li>
 *   <li>{@code @AttributeOverride}로 컬럼명 매핑 정상 (write_at/amended_at)</li>
 *   <li>{@code @EnableJpaAuditing}이 MainApplication에서 활성화됨 (Phase 3-A에서 검증, 재확인)</li>
 * </ol>
 *
 * <p>Phase 3-Backfill-B-2: postReview / deleteReview happy path 추가.
 * Service 직접 호출 + DB 재조회 검증 (BaseEntity 검증 패턴 일관 — INSERT → findById/JPQL 재조회).</p>
 */
@SpringBootTest
@Transactional
@Rollback
class ReviewControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ReviewService reviewService;
    @PersistenceContext private EntityManager entityManager;

    private static final long TEST_USER_NO = 99999L;
    private static final long TEST_STORE_ID = 21L;  // 실재 store (BaseEntity 검증 fixture와 일관)

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private Orders seedOrder(long orderId) {
        Orders order = new Orders();
        order.setOrderId(orderId);
        order.setUserNo(TEST_USER_NO);
        order.setStoreId(TEST_STORE_ID);
        order.setRequest("테스트");
        order.setRiderRequest("테스트");
        order.setAddress("테스트 주소");
        order.setAddressDetail("테스트 상세");
        order.setDeliveryFee(1500);
        order.setAmount(15000);
        order.setDeliveryState(1);
        order.setPayState(1);
        order.setOrderState(1);
        return orderRepository.saveAndFlush(order);
    }

    @Test
    @DisplayName("BaseEntity 첫 검증 — Review.saveAndFlush 후 write_at/amended_at 자동 채움")
    void review_baseEntityAuditing() {
        // 1. 임시 orders row INSERT (review.order_id FK 만족용)
        long testOrderId = 999999999L;
        seedOrder(testOrderId);

        // 2. Review INSERT — BaseEntity Auditing 검증
        Review review = new Review();
        review.setOrderId(testOrderId);
        review.setRating(5);
        review.setContents("BaseEntity audit 검증 리뷰");
        review.setPhoto(null);
        reviewRepository.saveAndFlush(review);

        // 3. 검증
        assertThat(review.getReviewId()).isNotNull();
        assertThat(review.getCreatedAt())
                .as("@CreatedDate → write_at 자동 채움 (@AttributeOverride)")
                .isNotNull();
        assertThat(review.getUpdatedAt())
                .as("@LastModifiedDate → amended_at 자동 채움 (@AttributeOverride)")
                .isNotNull();

        // 4. Repository.findById로 재조회 — DB에서 읽어와도 createdAt/updatedAt 보존 확인
        Review fetched = reviewRepository.findById(review.getReviewId()).orElseThrow();
        assertThat(fetched.getCreatedAt()).isNotNull();
        assertThat(fetched.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getReviewById 미존재 — 인증 X endpoint, NOT_FOUND BusinessException")
    void getReviewById_notFound() throws Exception {
        long nonexistentReviewId = 99999999L;
        MvcResult result = mockMvc.perform(get("/api/user/review/" + nonexistentReviewId))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        SnapshotAssert.assertMatches("review-get-by-id-not-found",
                result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("postReview happy — 본인 주문 리뷰 작성 → DB 반영 + write_at/amended_at 자동 채움")
    void postReview_happy() {
        // 1. fixture: 본인 주문 INSERT
        long testOrderId = 999999997L;
        seedOrder(testOrderId);

        // 2. happy: 본인 주문 → postReview (Service 직접 호출)
        ReviewReq req = new ReviewReq();
        req.setOrderId(testOrderId);
        req.setUserNo(TEST_USER_NO);  // ReviewReq.userNo는 미사용 (callerUserNo로 위조 방지)
        req.setText("happy path 통합 검증");
        req.setRating(5);
        req.setImage(null);
        reviewService.postReview(TEST_USER_NO, req);

        // 3. 검증: orderId로 review row 재조회 (1차 캐시 우회)
        entityManager.flush();
        entityManager.clear();
        List<Review> reviews = entityManager.createQuery(
                        "SELECT r FROM Review r WHERE r.orderId = :oid", Review.class)
                .setParameter("oid", testOrderId)
                .getResultList();
        assertThat(reviews)
                .as("postReview 후 review row 1건 INSERT")
                .hasSize(1);
        Review saved = reviews.get(0);
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getContents()).isEqualTo("happy path 통합 검증");
        assertThat(saved.getPhoto()).isNull();
        assertThat(saved.getCreatedAt())
                .as("@CreatedDate write_at 자동 채움")
                .isNotNull();
        assertThat(saved.getUpdatedAt())
                .as("@LastModifiedDate amended_at 자동 채움")
                .isNotNull();
    }

    @Test
    @DisplayName("deleteReview happy — 본인 리뷰 삭제 → DB row 제거")
    void deleteReview_happy() {
        // 1. fixture: 본인 주문 + review INSERT
        long testOrderId = 999999996L;
        seedOrder(testOrderId);

        Review review = new Review();
        review.setOrderId(testOrderId);
        review.setRating(4);
        review.setContents("삭제 대상 리뷰");
        reviewRepository.saveAndFlush(review);
        Long fixtureReviewId = review.getReviewId();
        assertThat(fixtureReviewId).isNotNull();

        // 2. happy: 본인 리뷰 → deleteReview (Service 직접 호출)
        reviewService.deleteReview(TEST_USER_NO, fixtureReviewId);

        // 3. 검증: 1차 캐시 비우고 DB 재조회 → 사라짐 확인
        entityManager.flush();
        entityManager.clear();
        assertThat(reviewRepository.findById(fixtureReviewId))
                .as("deleteReview 후 review row 제거됨")
                .isEmpty();
    }
}
