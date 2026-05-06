package com.green.mmg.main.cart;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.cart.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-D Step D-3: CartService 권한 분기 추가 + 단위 테스트.
 *
 * <p>모든 메서드는 {@code callerUserNo} (JWT principal) + 본인 자원 검증을 거친다.
 * cartItem 검증은 cartId → Cart.userNo 조회 후 비교.</p>
 *
 * <p>학원 DB / Spring 컨텍스트 의존 0 — 순수 Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService — 권한 분기 + 분기별 동작")
class CartServiceTest {

    @Mock private CartMapper cartMapper;
    @Mock private CartRepository cartRepository;
    @Mock private CartDetailRepository cartDetailRepository;

    @InjectMocks
    private CartService cartService;

    private static final Long USER_NO = 42L;
    private static final Long OTHER_USER_NO = 99L;
    private static final Long STORE_ID_21 = 21L;
    private static final Long STORE_ID_22 = 22L;
    private static final Long MENU_ID = 17L;
    private static final Long CART_ID = 700L;
    private static final Long CART_ITEM_ID = 9001L;

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getCart — 권한 + JPA findByUserNo + MyBatis 합성")
    class GetCart {

        @Test
        @DisplayName("happy: callerUserNo == userNo → CartListRes 동결")
        void happyPath_assemblesResponse() {
            Cart cart = newCart(CART_ID, USER_NO, STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(cart));
            when(cartMapper.findStoreNameByStoreId(STORE_ID_21)).thenReturn("숨은집피자");
            CartItemRes item = new CartItemRes();
            item.setMenuId(MENU_ID);
            item.setQuantity(2);
            when(cartMapper.findCartItems(CART_ID)).thenReturn(List.of(item));

            CartListRes res = cartService.getCart(USER_NO, USER_NO);

            assertThat(res.getCartId()).isEqualTo(CART_ID);
            assertThat(res.getStoreId()).isEqualTo(STORE_ID_21);
            assertThat(res.getStoreName()).isEqualTo("숨은집피자");
            assertThat(res.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("403: callerUserNo != userNo → FORBIDDEN '본인 장바구니만 접근 가능합니다.' + Repository 미호출")
        void otherUser_throwsForbidden() {
            assertThatThrownBy(() -> cartService.getCart(OTHER_USER_NO, USER_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 장바구니만 접근 가능합니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verifyNoInteractions(cartRepository);
            verifyNoInteractions(cartMapper);
        }

        @Test
        @DisplayName("happy: 본인 cart 없음 → null 반환 + Mapper 미호출")
        void noCart_returnsNullAndShortCircuits() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            CartListRes res = cartService.getCart(USER_NO, USER_NO);

            assertThat(res).isNull();
            verifyNoInteractions(cartMapper);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addToCart — 권한 + 분기별 동작")
    class AddToCart {

        @Test
        @DisplayName("happy 신규: caller==dto.userNo → Cart save + CartDetail save")
        void newCart_savesBoth() {
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 2);
            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            cartService.addToCart(USER_NO, dto);

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
        @DisplayName("403: caller != dto.userNo → FORBIDDEN + 모든 후속 호출 미발생")
        void otherUser_throwsForbiddenAndShortCircuits() {
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 2);

            assertThatThrownBy(() -> cartService.addToCart(OTHER_USER_NO, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 장바구니만 접근 가능합니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verifyNoInteractions(cartMapper);
            verifyNoInteractions(cartRepository);
            verifyNoInteractions(cartDetailRepository);
        }

        @Test
        @DisplayName("happy 같은 메뉴: 같은 매장 + 기존 아이템 → quantity 합산 (dirty)")
        void existingItem_quantityIncremented() {
            Cart existing = newCart(CART_ID, USER_NO, STORE_ID_21);
            CartDetail existingItem = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 2);
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 3);

            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(existing));
            when(cartDetailRepository.findByCartIdAndMenuId(CART_ID, MENU_ID))
                    .thenReturn(Optional.of(existingItem));

            cartService.addToCart(USER_NO, dto);

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

            cartService.addToCart(USER_NO, dto);

            ArgumentCaptor<CartDetail> captor = ArgumentCaptor.forClass(CartDetail.class);
            verify(cartDetailRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getQuantity()).isEqualTo(1);
            verify(cartRepository, never()).saveAndFlush(any(Cart.class));
        }

        @Test
        @DisplayName("예외: 메뉴 없음 (storeId==null) → RuntimeException + cart/detail 저장 미발생")
        void menuNotFound_throws() {
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 1);
            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(null);

            assertThatThrownBy(() -> cartService.addToCart(USER_NO, dto))
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

            assertThatThrownBy(() -> cartService.addToCart(USER_NO, dto))
                    .isInstanceOf(DifferentStoreException.class)
                    .hasMessageContaining("다른 매장");

            verifyNoInteractions(cartDetailRepository);
            verify(cartRepository, never()).saveAndFlush(any(Cart.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("clearAndAddToCart — 권한 + 카트 비우고 새 메뉴")
    class ClearAndAddToCart {

        @Test
        @DisplayName("happy 기존 카트 있음: 권한 통과 → detail/cart 삭제 + 신규 저장")
        void existingCart_clearedAndReplaced() {
            Cart existing = newCart(CART_ID, USER_NO, STORE_ID_22);
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 2);

            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(existing));

            cartService.clearAndAddToCart(USER_NO, dto);

            verify(cartDetailRepository).deleteByCartId(CART_ID);
            verify(cartRepository).delete(existing);
            verify(cartRepository).flush();

            ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).saveAndFlush(cartCaptor.capture());
            assertThat(cartCaptor.getValue().getStoreId()).isEqualTo(STORE_ID_21);
        }

        @Test
        @DisplayName("403: caller != dto.userNo → FORBIDDEN + 모든 호출 미발생")
        void otherUser_throwsForbidden() {
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 2);

            assertThatThrownBy(() -> cartService.clearAndAddToCart(OTHER_USER_NO, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 장바구니만 접근 가능합니다.");

            verifyNoInteractions(cartMapper);
            verifyNoInteractions(cartRepository);
            verifyNoInteractions(cartDetailRepository);
        }

        @Test
        @DisplayName("happy 기존 카트 없음: 삭제 단계 skip + 신규 cart/detail 저장")
        void noExistingCart_savesOnly() {
            CartAddRequestDto dto = addReq(USER_NO, MENU_ID, 1);
            when(cartMapper.findStoreIdByMenuId(MENU_ID)).thenReturn(STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            cartService.clearAndAddToCart(USER_NO, dto);

            verify(cartDetailRepository, never()).deleteByCartId(any());
            verify(cartRepository, never()).delete(any(Cart.class));
            verify(cartRepository).saveAndFlush(any(Cart.class));
            verify(cartDetailRepository).saveAndFlush(any(CartDetail.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateCartItem — cartItem 소유자 검증 + dirty checking")
    class UpdateCartItem {

        @Test
        @DisplayName("happy: 본인 cart → quantity setter (dirty) — 명시적 save 없음 동결")
        void happyPath_dirtyCheckingOnly() {
            CartDetail item = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 2);
            Cart cart = newCart(CART_ID, USER_NO, STORE_ID_21);
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart));

            cartService.updateCartItem(USER_NO, CART_ITEM_ID, 7);

            assertThat(item.getQuantity()).isEqualTo(7);
            verify(cartDetailRepository, never()).save(any());
            verify(cartDetailRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("403: 다른 사용자 cartItem → FORBIDDEN '본인 장바구니 아이템만 접근 가능합니다.' + quantity 미변경")
        void otherUserCartItem_throwsForbidden() {
            CartDetail item = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 2);
            Cart cart = newCart(CART_ID, OTHER_USER_NO, STORE_ID_21);  // cart는 다른 사용자 소유
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart));

            assertThatThrownBy(() -> cartService.updateCartItem(USER_NO, CART_ITEM_ID, 7))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 장바구니 아이템만 접근 가능합니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            assertThat(item.getQuantity()).isEqualTo(2);  // dirty checking 미발생 동결
        }

        @Test
        @DisplayName("예외: cartItem 없음 → RuntimeException '장바구니 아이템 없음' + cart 조회 미발생")
        void itemNotFound_throws() {
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateCartItem(USER_NO, CART_ITEM_ID, 5))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("장바구니 아이템 없음");

            verify(cartRepository, never()).findById(any());
        }

        @Test
        @DisplayName("예외: cart 없음 (FK 깨짐) → BusinessException NOT_FOUND '장바구니를 찾을 수 없습니다.'")
        void cartNotFound_throwsNotFound() {
            CartDetail item = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 2);
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateCartItem(USER_NO, CART_ITEM_ID, 7))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("장바구니를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteCartItem — cartItem 소유자 검증 + 마지막 아이템 cart 삭제")
    class DeleteCartItem {

        @Test
        @DisplayName("happy: 본인 cart + 마지막 아이템 → countByCartId==0 → cart도 deleteById")
        void lastItem_alsoDeletesCart() {
            CartDetail item = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 1);
            Cart cart = newCart(CART_ID, USER_NO, STORE_ID_21);
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart));
            when(cartDetailRepository.countByCartId(CART_ID)).thenReturn(0L);

            cartService.deleteCartItem(USER_NO, CART_ITEM_ID);

            verify(cartDetailRepository).delete(item);
            verify(cartDetailRepository).flush();
            verify(cartRepository).deleteById(CART_ID);
        }

        @Test
        @DisplayName("403: 다른 사용자 cartItem → FORBIDDEN + delete 미호출 (DB 미변경 동결)")
        void otherUserCartItem_throwsForbiddenAndShortCircuits() {
            CartDetail item = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 1);
            Cart cart = newCart(CART_ID, OTHER_USER_NO, STORE_ID_21);
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart));

            assertThatThrownBy(() -> cartService.deleteCartItem(USER_NO, CART_ITEM_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 장바구니 아이템만 접근 가능합니다.");

            verify(cartDetailRepository, never()).delete(any(CartDetail.class));
            verify(cartDetailRepository, never()).countByCartId(any());
            verify(cartRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("happy: 다른 아이템 남음 → countByCartId>0 → cart 유지")
        void itemsRemain_cartKept() {
            CartDetail item = newDetail(CART_ITEM_ID, CART_ID, MENU_ID, 1);
            Cart cart = newCart(CART_ID, USER_NO, STORE_ID_21);
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart));
            when(cartDetailRepository.countByCartId(CART_ID)).thenReturn(2L);

            cartService.deleteCartItem(USER_NO, CART_ITEM_ID);

            verify(cartDetailRepository).delete(item);
            verify(cartRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("예외: cartItem 없음 → RuntimeException + 권한 검증/삭제 미호출")
        void itemNotFound_throwsAndShortCircuits() {
            when(cartDetailRepository.findById(CART_ITEM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.deleteCartItem(USER_NO, CART_ITEM_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("장바구니 아이템 없음");

            verify(cartRepository, never()).findById(any());
            verify(cartDetailRepository, never()).delete(any(CartDetail.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("clearCart — 권한 + 사용자 카트 통째 삭제")
    class ClearCart {

        @Test
        @DisplayName("happy: 본인 카트 있음 → detail 전부 + cart 삭제")
        void cartExists_deletesBoth() {
            Cart existing = newCart(CART_ID, USER_NO, STORE_ID_21);
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(existing));

            cartService.clearCart(USER_NO, USER_NO);

            verify(cartDetailRepository).deleteByCartId(CART_ID);
            verify(cartRepository).delete(existing);
        }

        @Test
        @DisplayName("403: caller != userNo → FORBIDDEN + 모든 호출 미발생")
        void otherUser_throwsForbidden() {
            assertThatThrownBy(() -> cartService.clearCart(OTHER_USER_NO, USER_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 장바구니만 접근 가능합니다.");

            verifyNoInteractions(cartRepository);
            verifyNoInteractions(cartDetailRepository);
        }

        @Test
        @DisplayName("happy: 본인이지만 카트 없음 → no-op")
        void noCart_noOp() {
            when(cartRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            cartService.clearCart(USER_NO, USER_NO);

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
