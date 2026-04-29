package com.green.mmg.main.payment;

import com.green.mmg.main.cart.CartDetailRepository;
import com.green.mmg.main.cart.CartRepository;
import com.green.mmg.main.cart.model.Cart;
import com.green.mmg.main.order.OrderRepository;
import com.green.mmg.main.order.model.Orders;
import com.green.mmg.main.payment.model.PaymentConfirmReq;
import com.green.mmg.main.payment.model.PaymentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-A: PaymentService.confirmPayment 흐름 동결 단위 테스트.
 *
 * <p>토스 외부 호출(callTossConfirm protected)은 Spy로 stub. HTTP/네트워크 의존 0.</p>
 *
 * <p>흐름 검증 핵심:
 * <ol>
 *   <li>주문 검증 실패 시 토스 호출 / 결제 저장 / 상태 변경 / 장바구니 정리 모두 미발생</li>
 *   <li>토스 실패 시 결제 저장 / 상태 변경 / 장바구니 정리 모두 미발생</li>
 *   <li>happy path는 결제 저장 → 상태 변경 → 장바구니 정리 순서로 발생</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService.confirmPayment 단위 테스트")
class PaymentServiceTest {

    @Mock private PaymentMapper paymentMapper;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartDetailRepository cartDetailRepository;

    @Spy
    @InjectMocks
    private PaymentService paymentService;

    private static final Long ORDER_ID = 39_177_544_005_887_23L;
    private static final Long USER_NO = 42L;
    private static final int AMOUNT = 16_500;

    private Orders order;
    private PaymentConfirmReq req;

    @BeforeEach
    void setUp() {
        order = new Orders();
        order.setOrderId(ORDER_ID);
        order.setUserNo(USER_NO);
        order.setStoreId(21L);
        order.setAmount(AMOUNT);
        order.setPayState(1);  // 미결제

        req = new PaymentConfirmReq();
        req.setOrderId(String.valueOf(ORDER_ID));
        req.setPaymentKey("test_payment_key");
        req.setAmount(AMOUNT);
        req.setPayState(1);
    }

    /** 토스 호출 성공 stub — happy path 케이스에서만 호출 */
    private void stubTossSuccess() throws Exception {
        doReturn(new org.json.simple.JSONObject()).when(paymentService).callTossConfirm(any());
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("주문 검증")
    class OrderValidation {

        @Test
        @DisplayName("주문 미존재 → '존재하지 않는 주문' 예외 + 토스 미호출 + DB 변경 0")
        void orderNotFound() throws Exception {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.confirmPayment(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("존재하지 않는 주문입니다.");

            verify(paymentService, never()).callTossConfirm(any());
            verify(paymentRepository, never()).save(any());
            verify(cartRepository, never()).delete(any());
            assertThat(order.getPayState()).isEqualTo(1);  // 변경 없음
        }

        @Test
        @DisplayName("결제 금액 불일치 → '결제 금액이 일치하지 않습니다' + 토스 미호출 + DB 변경 0")
        void amountMismatch() throws Exception {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            req.setAmount(AMOUNT + 1);  // 1원 차이

            assertThatThrownBy(() -> paymentService.confirmPayment(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("결제 금액이 일치하지 않습니다.");

            verify(paymentService, never()).callTossConfirm(any());
            verify(paymentRepository, never()).save(any());
            assertThat(order.getPayState()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 결제된 주문 → '이미 결제된 주문' + 토스 미호출 + DB 변경 0")
        void alreadyPaid() throws Exception {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentMapper.existsByOrderId(ORDER_ID)).thenReturn(true);

            assertThatThrownBy(() -> paymentService.confirmPayment(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("이미 결제된 주문입니다.");

            verify(paymentService, never()).callTossConfirm(any());
            verify(paymentRepository, never()).save(any());
            assertThat(order.getPayState()).isEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("토스 호출 단계")
    class TossCall {

        @Test
        @DisplayName("토스 실패 → 예외 그대로 전파 + 결제 미저장 + 주문 상태 미변경 + 장바구니 미정리")
        void tossFailure_propagates() throws Exception {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentMapper.existsByOrderId(ORDER_ID)).thenReturn(false);
            doThrow(new RuntimeException("INVALID_CARD_NUMBER")).when(paymentService).callTossConfirm(any());

            assertThatThrownBy(() -> paymentService.confirmPayment(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("INVALID_CARD_NUMBER");

            verify(paymentRepository, never()).save(any());
            verify(cartRepository, never()).delete(any());
            verify(cartDetailRepository, never()).deleteByCartId(any());
            assertThat(order.getPayState()).isEqualTo(1);  // 미변경
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("정상 결제 → save(payment) + order.payState=2 + cart 정리 (cart 보유 시)")
        void allStepsExecuted() throws Exception {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentMapper.existsByOrderId(ORDER_ID)).thenReturn(false);
            stubTossSuccess();

            Cart cart = new Cart();
            cart.setCartId(700L);
            cart.setUserNo(USER_NO);
            cart.setStoreId(21L);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(cart));

            paymentService.confirmPayment(req);

            // 결제 row 저장 — 필드 정확히 검증
            ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
            verify(paymentRepository).save(captor.capture());
            PaymentEntity saved = captor.getValue();
            assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(saved.getPaymentKey()).isEqualTo("test_payment_key");
            assertThat(saved.getAmount()).isEqualTo(AMOUNT);
            assertThat(saved.getPayState()).isEqualTo(1);

            // 주문 상태 = 결제완료
            assertThat(order.getPayState()).isEqualTo(2);

            // 장바구니 정리 — detail 먼저, cart 나중에
            verify(cartDetailRepository).deleteByCartId(700L);
            verify(cartRepository).delete(cart);
        }

        @Test
        @DisplayName("정상 결제이지만 cart 미존재 → 결제/상태는 OK, cart 정리 호출 0 (NPE 없음)")
        void noCart_noCleanupButPaymentSucceeds() throws Exception {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentMapper.existsByOrderId(ORDER_ID)).thenReturn(false);
            stubTossSuccess();
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            paymentService.confirmPayment(req);

            verify(paymentRepository).save(any(PaymentEntity.class));
            assertThat(order.getPayState()).isEqualTo(2);
            verify(cartDetailRepository, never()).deleteByCartId(any());
            verify(cartRepository, never()).delete(any(Cart.class));
        }

        @Test
        @DisplayName("결제 저장이 시작된 상태에서 paymentRepository.save 예외 → 주문 상태 미변경 + 장바구니 미정리")
        void saveFails_subsequentStepsSkipped() throws Exception {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentMapper.existsByOrderId(ORDER_ID)).thenReturn(false);
            stubTossSuccess();
            when(paymentRepository.save(any(PaymentEntity.class)))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException("unique key"));

            assertThatThrownBy(() -> paymentService.confirmPayment(req))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

            // 흐름 동결: save가 setPayState/cart 정리보다 먼저 — save 실패하면 이후 단계 미실행
            assertThat(order.getPayState()).isEqualTo(1);
            verify(cartRepository, never()).findByUserNo(any());
            verify(cartDetailRepository, never()).deleteByCartId(any());
            verify(cartRepository, never()).delete(any(Cart.class));
        }
    }
}
