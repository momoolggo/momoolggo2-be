package com.green.mmg.main.review;

import com.green.mmg.main.order.OrderRepository;
import com.green.mmg.main.order.model.Orders;
import com.green.mmg.main.review.model.Review;
import com.green.mmg.main.support.SnapshotAssert;
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
 * <p>BaseEntity 검증 실패 시 Phase 5 신규 도메인(펫/쿠폰/룰렛 등)에서 audit 컬럼 자유 사용 불가.</p>
 */
@SpringBootTest
@Transactional
@Rollback
class ReviewControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private OrderRepository orderRepository;

    private static final long TEST_USER_NO = 99999L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("BaseEntity 첫 검증 — Review.saveAndFlush 후 write_at/amended_at 자동 채움")
    void review_baseEntityAuditing() {
        // 1. 임시 orders row INSERT (review.order_id FK 만족용)
        long testOrderId = 999999999L;
        Orders testOrder = new Orders();
        testOrder.setOrderId(testOrderId);
        testOrder.setUserNo(TEST_USER_NO);
        testOrder.setStoreId(21L);  // 실재 store
        testOrder.setRequest("테스트");
        testOrder.setRiderRequest("테스트");
        testOrder.setAddress("테스트 주소");
        testOrder.setAddressDetail("테스트 상세");
        testOrder.setDeliveryFee(1500);
        testOrder.setAmount(15000);
        testOrder.setDeliveryState(1);
        testOrder.setPayState(1);
        testOrder.setOrderState(1);
        orderRepository.saveAndFlush(testOrder);

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
}
