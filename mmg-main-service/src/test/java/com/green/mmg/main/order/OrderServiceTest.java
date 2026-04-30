package com.green.mmg.main.order;

import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.address.UserAddressRepository;
import com.green.mmg.main.cart.CartMapper;
import com.green.mmg.main.cart.CartRepository;
import com.green.mmg.main.cart.model.Cart;
import com.green.mmg.main.cart.model.CartItemRes;
import com.green.mmg.main.order.model.OrderAddressInfo;
import com.green.mmg.main.order.model.OrderHistoryDto;
import com.green.mmg.main.order.model.OrderHistoryReq;
import com.green.mmg.main.order.model.OrderInfoRes;
import com.green.mmg.main.order.model.Orders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-B: OrderService 일반 메서드 단위 테스트 (calSumOrder 외).
 *
 * <p>학원 DB / Spring 컨텍스트 의존 0 — 순수 Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService — 일반 메서드 단위 테스트")
class OrderServiceTest {

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
    private static final int DELIVERY_FEE = 1500;

    private Cart cart;
    private List<CartItemRes> cartItems;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setCartId(CART_ID);
        cart.setUserNo(USER_NO);
        cart.setStoreId(STORE_ID);

        CartItemRes item1 = new CartItemRes();
        item1.setId(1L);
        item1.setMenuId(17L);
        item1.setMenuName("피자");
        item1.setQuantity(2);
        item1.setPrice(15000);

        CartItemRes item2 = new CartItemRes();
        item2.setId(2L);
        item2.setMenuId(18L);
        item2.setMenuName("콜라");
        item2.setQuantity(1);
        item2.setPrice(2000);

        cartItems = List.of(item1, item2);
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getOrderInfo — 주문 화면 초기 데이터")
    class GetOrderInfo {

        @Test
        @DisplayName("happy: 4개 외부 호출(cart/items/storeName/Feign tel/주소) 합성 + 금액 계산")
        void happyPath_assemblesAllSources() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(cart));
            when(cartMapper.findCartItems(CART_ID)).thenReturn(cartItems);
            when(cartMapper.findStoreNameByStoreId(STORE_ID)).thenReturn("숨은집피자");
            when(authFeignClient.getUser(USER_NO))
                    .thenReturn(new UserBriefDto(USER_NO, "준하", "010-1234-5678", ""));
            when(userAddressRepository.findFirstDefaultByUserNo(USER_NO))
                    .thenReturn(Optional.of(new OrderAddressInfo("서울시 강남구", "101동 202호")));

            OrderInfoRes res = orderService.getOrderInfo(USER_NO);

            assertThat(res.getStoreName()).isEqualTo("숨은집피자");
            assertThat(res.getTel()).isEqualTo("010-1234-5678");
            assertThat(res.getAddress()).isEqualTo("서울시 강남구");
            assertThat(res.getAddressDetail()).isEqualTo("101동 202호");
            assertThat(res.getItems()).hasSize(2);

            // 금액: (15000*2) + (2000*1) = 32000, +배달비 1500 = 33500
            assertThat(res.getMenuTotal()).isEqualTo(32_000);
            assertThat(res.getDeliveryFee()).isEqualTo(DELIVERY_FEE);
            assertThat(res.getTotalAmount()).isEqualTo(33_500);
        }

        @Test
        @DisplayName("기본 주소 없음 → address/addressDetail 빈 문자열")
        void noDefaultAddress_emptyStrings() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(cart));
            when(cartMapper.findCartItems(CART_ID)).thenReturn(cartItems);
            when(cartMapper.findStoreNameByStoreId(STORE_ID)).thenReturn("가게");
            when(authFeignClient.getUser(USER_NO))
                    .thenReturn(new UserBriefDto(USER_NO, "준하", "010-1111-2222", ""));
            when(userAddressRepository.findFirstDefaultByUserNo(USER_NO)).thenReturn(Optional.empty());

            OrderInfoRes res = orderService.getOrderInfo(USER_NO);

            assertThat(res.getAddress()).isEmpty();
            assertThat(res.getAddressDetail()).isEmpty();
            // 그 외 필드는 정상
            assertThat(res.getTel()).isEqualTo("010-1111-2222");
        }

        @Test
        @DisplayName("cart 없음 → '장바구니가 비어있습니다.' + Feign 미호출")
        void noCart_throwsAndShortCircuits() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderInfo(USER_NO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("장바구니가 비어있습니다");

            verifyNoInteractions(authFeignClient);
            verifyNoInteractions(userAddressRepository);
            verify(cartMapper, never()).findCartItems(anyLong());
        }

        @Test
        @DisplayName("items 0개 → '장바구니가 비어있습니다.' + Feign/주소 조회 미호출")
        void emptyItems_throwsAndShortCircuits() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(cart));
            when(cartMapper.findCartItems(CART_ID)).thenReturn(List.of());

            assertThatThrownBy(() -> orderService.getOrderInfo(USER_NO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("장바구니가 비어있습니다");

            verifyNoInteractions(authFeignClient);
            verifyNoInteractions(userAddressRepository);
        }

        @Test
        @DisplayName("Feign null → BusinessException NOT_FOUND '사용자 정보를 찾을 수 없습니다.' (storeOneGet 패턴 전파)")
        void feignNull_throwsNotFound() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(cart));
            when(cartMapper.findCartItems(CART_ID)).thenReturn(cartItems);
            when(cartMapper.findStoreNameByStoreId(STORE_ID)).thenReturn("가게");
            when(authFeignClient.getUser(USER_NO)).thenReturn(null);

            assertThatThrownBy(() -> orderService.getOrderInfo(USER_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("사용자 정보를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

            // setOwnerName/setTel 미실행 + 후속 주소 조회 미발생 동결
            verifyNoInteractions(userAddressRepository);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getOrderHistory — 권한 + 주문 내역 (orderMapper + items 합성)")
    class GetOrderHistory {

        @Test
        @DisplayName("happy: req.userId == caller → N개 주문 + items 합성")
        void historyAssembledWithItems() {
            OrderHistoryReq req = new OrderHistoryReq(USER_NO, 1, 10);

            OrderHistoryDto o1 = new OrderHistoryDto();
            o1.setOrderId(391_000_001L);
            OrderHistoryDto o2 = new OrderHistoryDto();
            o2.setOrderId(391_000_002L);
            when(orderMapper.findOrdersByUserId(req)).thenReturn(List.of(o1, o2));

            List<OrderHistoryDto.OrderItemDto> items1 = List.of(
                    new OrderHistoryDto.OrderItemDto("피자", 2, 15000));
            List<OrderHistoryDto.OrderItemDto> items2 = List.of(
                    new OrderHistoryDto.OrderItemDto("치킨", 1, 18000));
            when(orderDetailRepository.findItemsByOrderId(391_000_001L)).thenReturn(items1);
            when(orderDetailRepository.findItemsByOrderId(391_000_002L)).thenReturn(items2);

            List<OrderHistoryDto> result = orderService.getOrderHistory(USER_NO, req);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getItems().get(0).getName()).isEqualTo("피자");
            verify(orderDetailRepository, times(2)).findItemsByOrderId(anyLong());
        }

        @Test
        @DisplayName("403 위조: req.userId != caller → FORBIDDEN '본인 주문 내역만 조회 가능합니다.' + Mapper/Repository 미호출")
        void otherUserId_throwsForbidden() {
            OrderHistoryReq req = new OrderHistoryReq(999L, 1, 10);  // 위조 userId

            assertThatThrownBy(() -> orderService.getOrderHistory(USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 주문 내역만 조회 가능합니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(orderMapper, never()).findOrdersByUserId(any());
            verify(orderDetailRepository, never()).findItemsByOrderId(anyLong());
        }

        @Test
        @DisplayName("happy: 주문 0개 → orderDetailRepository 호출 0 + 빈 리스트")
        void noOrders_emptyListReturned() {
            OrderHistoryReq req = new OrderHistoryReq(USER_NO, 1, 10);
            when(orderMapper.findOrdersByUserId(req)).thenReturn(List.of());

            List<OrderHistoryDto> result = orderService.getOrderHistory(USER_NO, req);

            assertThat(result).isEmpty();
            verify(orderDetailRepository, never()).findItemsByOrderId(anyLong());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("orderHistoryDetail — 권한 + 주문 상세")
    class OrderHistoryDetailNested {

        @Test
        @DisplayName("happy: 본인 주문 → orderMapper 결과에 items 합성")
        void detail_assembledWithItems() {
            long orderId = 391_000_001L;
            Orders order = new Orders();
            order.setOrderId(orderId);
            order.setUserNo(USER_NO);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            OrderHistoryDto dto = new OrderHistoryDto();
            dto.setOrderId(orderId);
            dto.setStoreName("가게A");
            when(orderMapper.orderHistoryDetail(orderId)).thenReturn(dto);

            List<OrderHistoryDto.OrderItemDto> items = List.of(
                    new OrderHistoryDto.OrderItemDto("피자", 1, 15000));
            when(orderDetailRepository.findItemsByOrderId(orderId)).thenReturn(items);

            OrderHistoryDto result = orderService.orderHistoryDetail(USER_NO, orderId);

            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("403: 다른 사용자 주문 상세 시도 → FORBIDDEN '본인 주문만 조회 가능합니다.' + Mapper 미호출")
        void otherUserOrder_throwsForbidden() {
            long orderId = 391_000_001L;
            Orders order = new Orders();
            order.setOrderId(orderId);
            order.setUserNo(999L);  // 타인 소유
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.orderHistoryDetail(USER_NO, orderId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 주문만 조회 가능합니다.");

            verify(orderMapper, never()).orderHistoryDetail(anyLong());
            verify(orderDetailRepository, never()).findItemsByOrderId(anyLong());
        }

        @Test
        @DisplayName("404: orderId 미존재 → NOT_FOUND '주문을 찾을 수 없습니다.'")
        void orderNotFound_throwsNotFound() {
            long orderId = 999_999L;
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.orderHistoryDetail(USER_NO, orderId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("주문을 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("maxHistoryPage — 권한 + 주문 내역 최대 페이지")
    class MaxHistoryPage {

        @Test
        @DisplayName("happy: path userId == caller → countByUserNo 위임 (응답 동결: int)")
        void delegatesToCountByUserNo() {
            when(orderRepository.countByUserNo(USER_NO)).thenReturn(7L);

            int result = orderService.maxHistoryPage(USER_NO, USER_NO);

            assertThat(result).isEqualTo(7);
            verify(orderRepository).countByUserNo(USER_NO);
            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("403: path userId != caller → FORBIDDEN + countByUserNo 미호출")
        void otherUserId_throwsForbidden() {
            assertThatThrownBy(() -> orderService.maxHistoryPage(USER_NO, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 주문 내역만 조회 가능합니다.");

            verify(orderRepository, never()).countByUserNo(anyLong());
        }

        @Test
        @DisplayName("happy: count=0 → 0 반환")
        void zeroCount_returnsZero() {
            when(orderRepository.countByUserNo(USER_NO)).thenReturn(0L);

            int result = orderService.maxHistoryPage(USER_NO, USER_NO);

            assertThat(result).isZero();
        }
    }
}
