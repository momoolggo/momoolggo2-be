package com.green.mmg.main.payment;

import com.green.mmg.main.order.OrderRepository;
import com.green.mmg.main.order.model.Orders;
import com.green.mmg.main.support.SnapshotAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Phase 2-Backfill-B: 학원 DB row PK 하드코딩 제거 → fixture+rollback 패턴으로 전환.
 *
 * <p>이전: ORDER_ID_UNPAID/ORDER_ID_PAID를 학원 DB에 미리 존재하는 row PK로 하드코딩 →
 * 누군가 row를 삭제/수정하면 CI 실패. 다른 통합 테스트(Cart/Order/Review/LikedStore/UserAddress)는
 * 모두 @Transactional+@Rollback+테스트 내부 fixture INSERT 패턴인데 Payment만 예외였음.</p>
 *
 * <p>지금: 동일 패턴으로 통일.
 * <ul>
 *   <li>{@code @Transactional + @Rollback} — 모든 INSERT는 트랜잭션 종료 시 롤백</li>
 *   <li>각 테스트 시작 시 {@code orderRepository.saveAndFlush}로 fixture INSERT</li>
 *   <li>orderId는 {@code System.currentTimeMillis()} 기반 unique로 충돌 회피</li>
 *   <li>TEST_USER_NO=99999L (논리 FK만, 실제 DB 외부 FK는 Phase 2-A에서 DROP됨)</li>
 * </ul></p>
 *
 * <p>토스 외부 호출 케이스는 본 테스트 범위 외 (3 케이스 모두 토스 호출 전 throw). 정상 흐름은
 * PaymentServiceTest의 Spy 기반 단위 테스트로 검증.</p>
 */
@SpringBootTest
@Transactional
@Rollback
class PaymentControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderRepository orderRepository;

    private static final long TEST_USER_NO = 99999L;
    /** orders.store_id FK는 store 테이블에 살아있음 (Phase 2-A는 user FK만 DROP). 학원 DB 실존 store. */
    private static final long TEST_STORE_ID = 21L;
    private static final int  AMOUNT_UNPAID = 14_500;
    private static final int  AMOUNT_PAID   = 16_500;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /** 미결제 Orders fixture INSERT — payState=1, unique orderId 반환 */
    private long insertUnpaidOrder(int amount) {
        return insertOrder(amount, 1);
    }

    /** 결제완료 Orders fixture INSERT — payState=2 (orders.pay_state>1 검사 통과 → existsByOrderId true) */
    private long insertPaidOrder(int amount) {
        return insertOrder(amount, 2);
    }

    private long insertOrder(int amount, int payState) {
        long orderId = Long.parseLong("99" + System.currentTimeMillis() + (long) (Math.random() * 1000));
        Orders order = new Orders();
        order.setOrderId(orderId);
        order.setUserNo(TEST_USER_NO);
        order.setStoreId(TEST_STORE_ID);
        order.setAddress("테스트 주소");
        order.setAddressDetail("테스트 상세");
        order.setDeliveryFee(1500);
        order.setAmount(amount);
        order.setPayState(payState);
        orderRepository.saveAndFlush(order);
        return orderId;
    }

    private static Map<String, Object> confirmReq(String orderId, int amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentKey", "test_key_irrelevant");
        body.put("orderId", orderId);
        body.put("amount", amount);
        body.put("payState", 1);
        return body;
    }

    @Test
    @DisplayName("미존재 orderId → 500 + '존재하지 않는 주문입니다.' (토스 호출 전 차단)")
    void confirmPayment_orderNotFound() throws Exception {
        // fixture 없음 — 임의 orderId 그대로 사용
        MvcResult result = mockMvc.perform(post("/api/payment/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq("999999999999", 1))))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        SnapshotAssert.assertMatches("payment-confirm-order-not-found",
                result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("금액 불일치 → 500 + '결제 금액이 일치하지 않습니다.'")
    void confirmPayment_amountMismatch() throws Exception {
        long orderId = insertUnpaidOrder(AMOUNT_UNPAID);

        MvcResult result = mockMvc.perform(post("/api/payment/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                confirmReq(String.valueOf(orderId), AMOUNT_UNPAID + 1))))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        SnapshotAssert.assertMatches("payment-confirm-amount-mismatch",
                result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("이미 결제됨 → 500 + '이미 결제된 주문입니다.' (existsByOrderId MyBatis 잔존 검증)")
    void confirmPayment_alreadyPaid() throws Exception {
        long orderId = insertPaidOrder(AMOUNT_PAID);

        MvcResult result = mockMvc.perform(post("/api/payment/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                confirmReq(String.valueOf(orderId), AMOUNT_PAID))))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        SnapshotAssert.assertMatches("payment-confirm-already-paid",
                result.getResponse().getContentAsString());
    }
}
