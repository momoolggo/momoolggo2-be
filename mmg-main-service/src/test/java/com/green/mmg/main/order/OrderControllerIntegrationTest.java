package com.green.mmg.main.order;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Phase 3-C-1 검증: Order/OrderDetail 단순 CRUD JPA 전환 응답 동결.
 *
 * <p>placeOrder/getOrderInfo/confirmPayment 정상 흐름은 인증 의존(@AuthenticationPrincipal)
 * + Cart 데이터 사전 셋업 필요 — 통합 테스트 범위 외. Phase 3-B 패턴 (JPA save + MyBatis JOIN)
 * 은 placeOrder에서도 동일 동작 (saveAndFlush + cartMapper.findCartItems).</p>
 *
 * <p>인증 불필요 endpoint 위주 + snapshot 캡처:</p>
 * <ul>
 *   <li>DELETE /api/order/{id}: orderRepository.deleteByOrderIdAndPayStateUnpaid 동작</li>
 *   <li>GET /api/order/history/max/{id}: orderRepository.countByUserNo 동작</li>
 *   <li>GET /api/order/history: orderMapper.findOrdersByUserId(MyBatis 잔존) +
 *       orderDetailRepository.findItemsByOrderId(JPA constructor expression)</li>
 * </ul>
 */
@SpringBootTest
@Transactional
@Rollback
class OrderControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;

    /** 미존재 user/order — 학원 DB 변경 0 보장 */
    private static final long TEST_USER_NO = 99999L;
    private static final long NONEXISTENT_ORDER_ID = 999999999999L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("deleteOrder 미존재 — pay_state=1 조건 미해당 → result=0 + 삭제실패")
    void deleteOrder_notFound() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/order/" + NONEXISTENT_ORDER_ID))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        SnapshotAssert.assertMatches("order-delete-not-found",
                result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("maxHistoryPage 미존재 user → 0 (countByUserNo)")
    void maxHistoryPage_zero() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/order/history/max/" + TEST_USER_NO))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        SnapshotAssert.assertMatches("order-max-history-zero",
                result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("getOrderHistory 빈 결과 — MyBatis findOrdersByUserId + JPA findItemsByOrderId 합성")
    void getOrderHistory_empty() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/order/history")
                        .param("userId", String.valueOf(TEST_USER_NO))
                        .param("startIdx", "0")
                        .param("size", "10"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        SnapshotAssert.assertMatches("order-history-empty",
                result.getResponse().getContentAsString());
    }
}
