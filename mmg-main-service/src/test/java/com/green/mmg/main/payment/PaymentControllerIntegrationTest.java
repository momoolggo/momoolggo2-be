package com.green.mmg.main.payment;

import com.green.mmg.main.support.SnapshotAssert;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Phase 3-B-1 검증: Payment 도메인 JPA 전환 후 응답 스펙 동결 확인.
 *
 * <p>토스 외부 API를 호출하는 정상 케이스는 제외 (사용자 결정 Q-Final-1=A).
 * confirmPayment의 검증 로직 3개(주문 부재/금액 불일치/이미 결제됨)만 통합 테스트.
 * 정상 흐름은 Phase 3-B 종료 시 수동 검증 1회 실행.</p>
 *
 * <p>학원 공유 DB 의존: orders 테이블의 기존 row 사용 (READ-ONLY, INSERT/UPDATE 없음).</p>
 */
@SpringBootTest
class PaymentControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    /** orders 테이블에서 사전 조회한 결제 전 주문 (pay_state=1) — READ-ONLY 사용 */
    private static final long ORDER_ID_UNPAID = 391775460588723L;
    private static final int  AMOUNT_UNPAID   = 14500;

    /** orders 테이블에서 사전 조회한 결제 완료 주문 (pay_state=2) — existsByOrderId 검증용 */
    private static final long ORDER_ID_PAID = 391774690588789L;
    private static final int  AMOUNT_PAID   = 16500;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("미존재 orderId → 500 + '존재하지 않는 주문입니다.' (토스 호출 전 차단)")
    void confirmPayment_orderNotFound() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentKey", "test_key_irrelevant");
        body.put("orderId", "999999999999");
        body.put("amount", 1);
        body.put("payState", 1);

        MvcResult result = mockMvc.perform(post("/api/payment/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        SnapshotAssert.assertMatches("payment-confirm-order-not-found",
                result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("금액 불일치 → 500 + '결제 금액이 일치하지 않습니다.'")
    void confirmPayment_amountMismatch() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentKey", "test_key_irrelevant");
        body.put("orderId", String.valueOf(ORDER_ID_UNPAID));
        body.put("amount", AMOUNT_UNPAID + 1);
        body.put("payState", 1);

        MvcResult result = mockMvc.perform(post("/api/payment/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        SnapshotAssert.assertMatches("payment-confirm-amount-mismatch",
                result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("이미 결제됨 → 500 + '이미 결제된 주문입니다.' (existsByOrderId MyBatis 잔존 검증)")
    void confirmPayment_alreadyPaid() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentKey", "test_key_irrelevant");
        body.put("orderId", String.valueOf(ORDER_ID_PAID));
        body.put("amount", AMOUNT_PAID);
        body.put("payState", 1);

        MvcResult result = mockMvc.perform(post("/api/payment/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        SnapshotAssert.assertMatches("payment-confirm-already-paid",
                result.getResponse().getContentAsString());
    }
}
