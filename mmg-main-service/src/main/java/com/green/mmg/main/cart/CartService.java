package com.green.mmg.main.cart;

import com.green.mmg.main.cart.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;

    public CartListRes getCart(Long userNo) {
        // 1. cart 기본 정보 조회
        Cart cart = cartMapper.findCartEntityByUserNo(userNo);
        if (cart == null) return null;

        // 2. 가게명 조회
        String storeName = cartMapper.findStoreNameByStoreId(cart.getStoreId());

        // 3. 아이템 목록 조회
        List<CartItemRes> items = cartMapper.findCartItems(cart.getCartId());

        // 4. CartListRes 조합
        CartListRes response = new CartListRes();
        response.setCartId(cart.getCartId());
        response.setStoreId(cart.getStoreId());
        response.setStoreName(storeName);
        response.setItems(items);

        return response;
    }

    // 장바구니 담기
    @Transactional
    public void addToCart(CartAddRequestDto dto) {
        Long storeId = cartMapper.findStoreIdByMenuId(dto.getMenuId());
        if (storeId == null) throw new RuntimeException("존재하지 않는 메뉴입니다.");

        Cart existCart = cartMapper.findCartEntityByUserNo(dto.getUserNo());

        if (existCart == null) {
            // 장바구니 없음 → 새로 생성
            cartMapper.insertCart(dto.getUserNo(), storeId);
            Long newCartId = cartMapper.getLastCartId();
            cartMapper.insertCartItem(newCartId, dto.getMenuId(), dto.getQuantity());

        } else if (existCart.getStoreId().equals(storeId)) {
            // 같은 매장 → 같은 메뉴 있는지 확인
            Long existItemId = cartMapper.findCartItemId(existCart.getCartId(), dto.getMenuId()); // ✅ 추가

            if (existItemId != null) {
                // 같은 메뉴 있음 → 수량 합산
                cartMapper.addCartItemQuantity(existItemId, dto.getQuantity()); // ✅ 추가
            } else {
                // 같은 메뉴 없음 → 새 행 추가
                cartMapper.insertCartItem(existCart.getCartId(), dto.getMenuId(), dto.getQuantity());
            }

        } else {
            throw new DifferentStoreException("다른 매장의 메뉴가 장바구니에 있습니다.");
        }
    }

    // 장바구니 비우고 새로 담기
    @Transactional
    public void clearAndAddToCart(CartAddRequestDto dto) {
        Long storeId = cartMapper.findStoreIdByMenuId(dto.getMenuId());
        Cart existCart = cartMapper.findCartEntityByUserNo(dto.getUserNo());
        if (existCart != null) {
            cartMapper.deleteAllCartItems(existCart.getCartId());
            cartMapper.deleteCart(existCart.getCartId());
        }
        cartMapper.insertCart(dto.getUserNo(), storeId);
        Long newCartId = cartMapper.getLastCartId();
        cartMapper.insertCartItem(newCartId, dto.getMenuId(), dto.getQuantity());
    }

    // 수량 변경
    @Transactional
    public void updateCartItem(Long cartItemId, int quantity) {
        cartMapper.updateCartItem(cartItemId, quantity);
    }

    // 단일 아이템 삭제
    @Transactional
    public void deleteCartItem(Long cartItemId) {
        // 1. 아이템 삭제 전 cart_id 조회
        Long cartId = cartMapper.findCartIdByCartItemId(cartItemId);

        // 2. 아이템 삭제
        cartMapper.deleteCartItem(cartItemId);

        // 3. cart에 아이템이 하나도 없으면 cart도 삭제
        int remainCount = cartMapper.countCartItems(cartId);
        if (remainCount == 0) {
            cartMapper.deleteCart(cartId);
        }
    }

    // 장바구니 비우기
    @Transactional
    public void clearCart(Long userNo) {
        Cart existCart = cartMapper.findCartEntityByUserNo(userNo);
        if (existCart != null) {
            cartMapper.deleteAllCartItems(existCart.getCartId());
            cartMapper.deleteCart(existCart.getCartId());
        }
    }
}