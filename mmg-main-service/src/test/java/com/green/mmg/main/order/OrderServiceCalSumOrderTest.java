package com.green.mmg.main.order;

import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.address.UserAddressRepository;
import com.green.mmg.main.cart.CartMapper;
import com.green.mmg.main.cart.CartRepository;
import com.green.mmg.main.cart.model.Cart;
import com.green.mmg.main.cart.model.CartItemRes;
import com.green.mmg.main.order.model.OrderReqDto;
import com.green.mmg.main.order.model.Orders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-A: calSumOrder 파라미터 버그 회귀 방지 단위 테스트.
 *
 * <p>이전: orderId를 calSumOrder에 전달 → SQL 서브쿼리로 store_id 도출.
 * deleteOrder 호출 직후엔 orders row가 없으므로 store.order_count 미갱신.</p>
 *
 * <p>지금: 호출자(placeOrder/deleteOrder)가 storeId를 명시 전달.
 * 본 테스트가 storeId 전달을 강제 동결.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService — calSumOrder 호출 검증")
class OrderServiceCalSumOrderTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private CartMapper cartMapper;
    @Mock private CartRepository cartRepository;
    @Mock private UserAddressRepository userAddressRepository;
    @Mock private AuthFeignClient authFeignClient;

    @InjectMocks
    private OrderService orderService;

    private static final long USER_NO = 42L;
    private static final long STORE_ID = 21L;
    private static final long CART_ID = 700L;

    private Cart cart;
    private List<CartItemRes> cartItems;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setCartId(CART_ID);
        cart.setUserNo(USER_NO);
        cart.setStoreId(STORE_ID);

        CartItemRes item = new CartItemRes();
        item.setId(1L);
        item.setMenuId(17L);
        item.setMenuName("피자");
        item.setQuantity(1);
        item.setPrice(15000);
        cartItems = List.of(item);
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

        @Test
        @DisplayName("주문 저장 후 calSumOrder가 cart.storeId로 정확히 호출됨")
        void calSumOrder_calledWithCartStoreId() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(cart));
            when(cartMapper.findCartItems(CART_ID)).thenReturn(cartItems);
            when(userAddressRepository.findFirstDefaultByUserNo(USER_NO)).thenReturn(Optional.empty());

            OrderReqDto dto = new OrderReqDto();
            dto.setRequest("벨 누르지 마세요");
            dto.setRiderRequest("문 앞에");
            dto.setPayState(1);

            long orderId = orderService.placeOrder(USER_NO, dto);

            assertThat(orderId).isGreaterThan(0L);
            verify(orderRepository).saveAndFlush(any(Orders.class));
            verify(orderMapper).calSumOrder(STORE_ID);  // ★ orderId 아닌 storeId 전달 동결
        }

        @Test
        @DisplayName("장바구니 비어있음 → 예외 + calSumOrder 미호출")
        void emptyCart_calSumOrderNotCalled() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            OrderReqDto dto = new OrderReqDto();
            dto.setPayState(1);

            assertThatThrownBy(() -> orderService.placeOrder(USER_NO, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("장바구니가 비어있습니다");

            verify(orderMapper, never()).calSumOrder(anyLong());
            verify(orderRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("cart는 있으나 items 0개 → 예외 + calSumOrder 미호출")
        void emptyItems_calSumOrderNotCalled() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(cart));
            when(cartMapper.findCartItems(CART_ID)).thenReturn(List.of());

            OrderReqDto dto = new OrderReqDto();
            dto.setPayState(1);

            assertThatThrownBy(() -> orderService.placeOrder(USER_NO, dto))
                    .isInstanceOf(RuntimeException.class);

            verify(orderMapper, never()).calSumOrder(anyLong());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteOrder")
    class DeleteOrder {

        @Test
        @DisplayName("삭제 성공(affected=1) → calSumOrder가 미리 확보한 storeId로 호출")
        void deleteSuccess_calSumOrderCalledWithStoreId() {
            long orderId = 39_175L;
            Orders order = new Orders();
            order.setOrderId(orderId);
            order.setStoreId(STORE_ID);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.deleteByOrderIdAndPayStateUnpaid(orderId)).thenReturn(1);

            int affected = orderService.deleteOrder(orderId);

            assertThat(affected).isEqualTo(1);
            // ★ 호출 순서: storeId 확보 → 삭제 → calSumOrder
            verify(orderRepository).findById(orderId);
            verify(orderRepository).deleteByOrderIdAndPayStateUnpaid(orderId);
            verify(orderMapper).calSumOrder(STORE_ID);  // ★ orderId 아닌 storeId
            verifyNoMoreInteractions(orderMapper);
        }

        @Test
        @DisplayName("삭제 0건(이미 결제됨 등) → calSumOrder 미호출")
        void deleteZero_calSumOrderNotCalled() {
            long orderId = 39_175L;
            Orders order = new Orders();
            order.setOrderId(orderId);
            order.setStoreId(STORE_ID);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.deleteByOrderIdAndPayStateUnpaid(orderId)).thenReturn(0);

            int affected = orderService.deleteOrder(orderId);

            assertThat(affected).isZero();
            verify(orderMapper, never()).calSumOrder(anyLong());
        }

        @Test
        @DisplayName("주문 미존재 → 삭제 0건 + calSumOrder 미호출 (이전 버그 핵심: row 삭제 후 store_id 도출 불가)")
        void orderNotFound_calSumOrderNotCalled() {
            long orderId = 99_999L;
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
            when(orderRepository.deleteByOrderIdAndPayStateUnpaid(orderId)).thenReturn(0);

            int affected = orderService.deleteOrder(orderId);

            assertThat(affected).isZero();
            verify(orderMapper, never()).calSumOrder(anyLong());
        }
    }
}
