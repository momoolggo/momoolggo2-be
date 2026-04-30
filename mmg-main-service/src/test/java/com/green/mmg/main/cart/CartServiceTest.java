package com.green.mmg.main.cart;

import com.green.mmg.main.cart.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-C: CartService 단위 테스트 — 현재 동작 동결.
 *
 * <p><b>권한 분기 부재를 명시적으로 동결한다.</b><br>
 * updateCartItem/deleteCartItem 등은 cartItemId만 받고 소유자 검증을 하지 않는다.
 * 다른 사용자 cartItem 접근 시도도 그대로 통과한다 — 이는 알려진 보안 부채이며
 * <b>Phase 2-Backfill-D에서 권한 분기를 추가할 예정</b>이다.<br>
 * 본 테스트는 현재 동작을 회귀 방지를 위해 그대로 동결한다 (D 단계에서 함께 갱신).</p>
 *
 * <p>학원 DB / Spring 컨텍스트 의존 0 — 순수 Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService — 단위 테스트 (현재 동작 동결)")
class CartServiceTest {

    @Mock private CartMapper cartMapper;
    @Mock private CartRepository cartRepository;
    @Mock private CartDetailRepository cartDetailRepository;

    @InjectMocks
    private CartService cartService;

    private static final Long USER_NO = 42L;
    private static final Long STORE_ID_21 = 21L;
    private static final Long STORE_ID_22 = 22L;
    private static final Long MENU_ID = 17L;
    private static final Long CART_ID = 700L;
    private static final Long CART_ITEM_ID = 9001L;

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getCart — JPA findByUserNo + MyBatis 합성")
    class GetCart {

        @Test
        @DisplayName("happy: cart 있음 → CartListRes 동결 (cartId/storeId/storeName/items)")
        void happyPath_assemblesResponse() {
            Cart cart = newCart(CART_ID, USER_NO, STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(cart));
            when(cartMapper.findStoreNameByStoreId(STORE_ID_21)).thenReturn("숨은집피자");
            CartItemRes item = new CartItemRes();
            item.setMenuId(MENU_ID);
            item.setQuantity(2);
            when(cartMapper.findCartItems(CART_ID)).thenReturn(List.of(item));

            CartListRes res = cartService.getCart(USER_NO);

            assertThat(res.getCartId()).isEqualTo(CART_ID);
            assertThat(res.getStoreId()).isEqualTo(STORE_ID_21);
            assertThat(res.getStoreName()).isEqualTo("숨은집피자");
            assertThat(res.getItems()).hasSize(1);
            assertThat(res.getItems().get(0).getMenuId()).isEqualTo(MENU_ID);
        }

        @Test
        @DisplayName("cart 없음 → null 반환 + Mapper 미호출 (현재 동작 동결)")
        void noCart_returnsNullAndShortCircuits() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            CartListRes res = cartService.getCart(USER_NO);

            assertThat(res).isNull();
            verifyNoInteractions(cartMapper);
            verifyNoInteractions(cartDetailRepository);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addToCart — 분기별 동작")
    class AddToCart {

        @Test
        @DisplayName("happy 신규: 카트 없음 → Cart save + CartDetail save (저장 필드 동결)")
        void newCart_savesBoth() {
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 2);
            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            cartService.addToCart(dto);

            ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).saveAndFlush(cartCaptor.capture());
            Cart savedCart = cartCaptor.getValue();
            assertThat(savedCart.getUserNo()).isEqualTo(USER_NO);
            assertThat(savedCart.getStoreId()).isEqualTo(STORE_ID_21);

            ArgumentCaptor<CartDetail> detailCaptor = ArgumentCaptor.forClass(CartDetail.class);
            verify(cartDetailRepository).saveAndFlush(detailCaptor.capture());
            CartDetail savedDetail = detailCaptor.getValue();
            assertThat(savedDetail.getMenuId()).isEqualTo(MENU_ID);
            assertThat(savedDetail.getQuantity()).isEqualTo(2);

            verify(cartDetailRepository, never()).findByCartIdAndMenuId(any(), any());
        }

        @Test
        @DisplayName("happy 같은 메뉴 재추가: 같은 매장 + 기존 아이템 → quantity 합산 (dirty)")
        void existingItem_quantityIncremented() {
            Cart existing = newCart(CART_ID, USER_NO, STORE_ID_21);
            CartDetail existingItem = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 2);
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 3);

            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(existing));
            when(cartDetailRepository.findByCartIdAndMenuId(CART_ID, MENU_ID))
                    .thenReturn(Optional.of(existingItem));

            cartService.addToCart(dto);

            assertThat(existingItem.getQuantity()).isEqualTo(5);
            verify(cartDetailRepository).saveAndFlush(existingItem);
            verify(cartRepository, never()).saveAndFlush(any(Cart.class));
        }

        @Test
        @DisplayName("happy 같은 매장 + 새 메뉴 → 새 CartDetail INSERT")
        void sameStoreNewMenu_savesNewDetail() {
            Cart existing = newCart(CART_ID, USER_NO, STORE_ID_21);
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 1);

            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(existing));
            when(cartDetailRepository.findByCartIdAndMenuId(CART_ID, MENU_ID))
                    .thenReturn(Optional.empty());

            cartService.addToCart(dto);

            ArgumentCaptor<CartDetail> captor = ArgumentCaptor.forClass(CartDetail.class);
            verify(cartDetailRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getCartId()).isEqualTo(CART_ID);
            assertThat(captor.getValue().getMenuId()).isEqualTo(MENU_ID);
            assertThat(captor.getValue().getQuantity()).isEqualTo(1);
            verify(cartRepository, never()).saveAndFlush(any(Cart.class));
        }

        @Test
        @DisplayName("예외: 메뉴 없음 (storeId==null) → RuntimeException + cart/detail 저장 미발생")
        void menuNotFound_throws() {
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 1);
            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(null);

            assertThatThrownBy(() -> cartService.addToCart(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("존재하지 않는 메뉴");

            verify(cartRepository, never()).findByUserNo(any());
            verifyNoInteractions(cartDetailRepository);
        }

        @Test
        @DisplayName("예외: 다른 매장 메뉴 추가 → DifferentStoreException + 저장 미발생")
        void differentStore_throws() {
            Cart existing = newCart(CART_ID, USER_NO, STORE_ID_21);
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 1);

            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(STORE_ID_22);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> cartService.addToCart(dto))
                    .isInstanceOf(DifferentStoreException.class)
                    .hasMessageContaining("다른 매장");

            verifyNoInteractions(cartDetailRepository);
            verify(cartRepository, never()).saveAndFlush(any(Cart.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("clearAndAddToCart — 카트 비우고 새 메뉴")
    class ClearAndAddToCart {

        @Test
        @DisplayName("happy 기존 카트 있음: detail 전부 삭제 + cart 삭제 + 신규 cart/detail 저장")
        void existingCart_clearedAndReplaced() {
            Cart existing = newCart(CART_ID, USER_NO, STORE_ID_22);
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 2);

            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(existing));

            cartService.clearAndAddToCart(dto);

            verify(cartDetailRepository).deleteByCartId(CART_ID);
            verify(cartRepository).delete(existing);
            verify(cartRepository).flush();

            ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).saveAndFlush(cartCaptor.capture());
            assertThat(cartCaptor.getValue().getUserNo()).isEqualTo(USER_NO);
            assertThat(cartCaptor.getValue().getStoreId()).isEqualTo(STORE_ID_21);

            ArgumentCaptor<CartDetail> detailCaptor = ArgumentCaptor.forClass(CartDetail.class);
            verify(cartDetailRepository).saveAndFlush(detailCaptor.capture());
            assertThat(detailCaptor.getValue().getMenuId()).isEqualTo(MENU_ID);
            assertThat(detailCaptor.getValue().getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("happy 기존 카트 없음: 삭제 단계 skip + 신규 cart/detail 저장")
        void noExistingCart_savesOnly() {
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 1);
            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            cartService.clearAndAddToCart(dto);

            verify(cartDetailRepository, never()).deleteByCartId(any());
            verify(cartRepository, never()).delete(any(Cart.class));
            verify(cartRepository).saveAndFlush(any(Cart.class));
            verify(cartDetailRepository).saveAndFlush(any(CartDetail.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateCartItem — quantity 수정 (dirty checking)")
    class UpdateCartItem {

        /**
         * 권한 분기 없음 동결: cartItemId만 받고 소유자 검증 X.
         * Phase 2-Backfill-D에서 userNo 파라미터 + 권한 분기 추가 예정.
         */
        @Test
        @DisplayName("happy: findById → quantity setter (dirty) — 명시적 save 호출 없음 동결")
        void happyPath_dirtyCheckingOnly() {
            CartDetail item = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 2);
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));

            cartService.updateCartItem(CART_ITEM_ID, 7);

            assertThat(item.getQuantity()).isEqualTo(7);
            verify(cartDetailRepository, never()).save(any());
            verify(cartDetailRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("예외: cartItem 없음 → RuntimeException '장바구니 아이템 없음'")
        void itemNotFound_throws() {
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateCartItem(CART_ITEM_ID, 5))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("장바구니 아이템 없음");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteCartItem — 마지막 아이템이면 cart도 삭제")
    class DeleteCartItem {

        /**
         * 권한 분기 없음 동결: cartItemId만 받고 소유자 검증 X.
         * Phase 2-Backfill-D에서 userNo 파라미터 + 권한 분기 추가 예정.
         */
        @Test
        @DisplayName("happy: 마지막 아이템 → countByCartId==0 → cart도 deleteById")
        void lastItem_alsoDeletesCart() {
            CartDetail item = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 1);
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));
            when(cartDetailRepository.countByCartId(CART_ID)).thenReturn(0L);

            cartService.deleteCartItem(CART_ITEM_ID);

            verify(cartDetailRepository).delete(item);
            verify(cartDetailRepository).flush();
            verify(cartDetailRepository).countByCartId(CART_ID);
            verify(cartRepository).deleteById(CART_ID);
        }

        @Test
        @DisplayName("happy: 다른 아이템 남음 → countByCartId>0 → cart 유지")
        void itemsRemain_cartKept() {
            CartDetail item = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 1);
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));
            when(cartDetailRepository.countByCartId(CART_ID)).thenReturn(2L);

            cartService.deleteCartItem(CART_ITEM_ID);

            verify(cartDetailRepository).delete(item);
            verify(cartRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("예외: cartItem 없음 → RuntimeException + 후속 호출 미발생")
        void itemNotFound_throwsAndShortCircuits() {
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.deleteCartItem(CART_ITEM_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("장바구니 아이템 없음");

            verify(cartDetailRepository, never()).delete(any(CartDetail.class));
            verify(cartDetailRepository, never()).countByCartId(any());
            verifyNoInteractions(cartRepository);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("clearCart — 사용자 카트 통째 삭제")
    class ClearCart {

        @Test
        @DisplayName("happy 카트 있음: detail 전부 + cart 삭제")
        void cartExists_deletesBoth() {
            Cart existing = newCart(CART_ID, USER_NO, STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(existing));

            cartService.clearCart(USER_NO);

            verify(cartDetailRepository).deleteByCartId(CART_ID);
            verify(cartRepository).delete(existing);
        }

        @Test
        @DisplayName("카트 없음: no-op (어떤 삭제도 호출 안 됨)")
        void noCart_noOp() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            cartService.clearCart(USER_NO);

            verify(cartDetailRepository, never()).deleteByCartId(any());
            verify(cartRepository, never()).delete(any(Cart.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    private static Cart newCart(Long cartId, Long userNo, Long storeId) {
        Cart c = new Cart();
        c.setCartId(cartId);
        c.setUserNo(userNo);
        c.setStoreId(storeId);
        return c;
    }

    private static CartDetail newDetail(Long itemId, Long cartId, Long menuId, int quantity) {
        CartDetail d = new CartDetail();
        d.setCartItemId(itemId);
        d.setCartId(cartId);
        d.setMenuId(menuId);
        d.setQuantity(quantity);
        return d;
    }

    private static CartAddRequestDto addReq(Long userNo, Long menuId, int quantity) {
        CartAddRequestDto dto = new CartAddRequestDto();
        dto.setUserNo(userNo);
        dto.setMenuId(menuId);
        dto.setQuantity(quantity);
        return dto;
    }
}
