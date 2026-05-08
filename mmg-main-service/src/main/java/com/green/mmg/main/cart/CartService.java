package com.green.mmg.main.cart;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.cart.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Phase 3-B-3: 하이브리드 영구 공존 핵심 검증 도메인.
 * Phase 2-Backfill-D Step D-3: cartItem 소유자 검증 추가 (보안).
 *
 * <p>모든 메서드는 호출자 userNo (JWT principal에서 추출)를 받고 본인 자원만 접근 가능.
 * 다른 사용자 카트/cartItem 접근 시도는 {@link BusinessException} FORBIDDEN.</p>
 *
 * <p>cartItem 소유자 검증: cartItemId로 CartDetail 조회 → cart의 userNo 비교.</p>
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;                  // MyBatis (JOIN 잔존)
    private final CartRepository cartRepository;          // JPA
    private final CartDetailRepository cartDetailRepository;  // JPA

    @Transactional(readOnly = true)
    public CartListRes getCart(long callerUserNo, Long userNo) {
        verifyOwner(callerUserNo, userNo);
        Cart cart = cartRepository.findByUserNo(userNo).orElse(null);
        if (cart == null) return null;

        // 가게명 + 아이템 목록은 MyBatis JOIN 잔존
        String storeName = cartMapper.findStoreNameByStoreId(cart.getStoreId());
        List<CartItemRes> items = cartMapper.findCartItems(cart.getCartId());

        CartListRes response = new CartListRes();
        response.setCartId(cart.getCartId());
        response.setStoreId(cart.getStoreId());
        response.setStoreName(storeName);
        response.setItems(items);

        int totalCount = items.stream()
                                .mapToInt(CartItemRes::getQuantity)
                                .sum();
        response.setCartCount(totalCount);

        return response;
    }

    @Transactional
    public void addToCart(long callerUserNo, CartAddRequestDto dto) {
        verifyOwner(callerUserNo, dto.getUserNo());
        Long storeId = cartMapper.findStoreIdByMenuId(dto.getMenuId());  // MyBatis JOIN
        if (storeId == null) throw new RuntimeException("존재하지 않는 메뉴입니다.");

        Cart existCart = cartRepository.findByUserNo(dto.getUserNo()).orElse(null);

        if (existCart == null) {
            // 새 카트 생성: save 후 MyBatis 후속 호출 가시화 위해 saveAndFlush
            Cart newCart = new Cart();
            newCart.setUserNo(dto.getUserNo());
            newCart.setStoreId(storeId);
            cartRepository.saveAndFlush(newCart);

            CartDetail detail = newDetail(newCart.getCartId(), dto.getMenuId(), dto.getQuantity());
            cartDetailRepository.saveAndFlush(detail);

        } else if (existCart.getStoreId().equals(storeId)) {
            Optional<CartDetail> existItem =
                    cartDetailRepository.findByCartIdAndMenuId(existCart.getCartId(), dto.getMenuId());

            if (existItem.isPresent()) {
                // 같은 메뉴 — 수량 합산 (dirty checking)
                CartDetail item = existItem.get();
                item.setQuantity(item.getQuantity() + dto.getQuantity());
                cartDetailRepository.saveAndFlush(item);
            } else {
                CartDetail detail = newDetail(existCart.getCartId(), dto.getMenuId(), dto.getQuantity());
                cartDetailRepository.saveAndFlush(detail);
            }

        } else {
            throw new DifferentStoreException("다른 매장의 메뉴가 장바구니에 있습니다.");
        }
    }

    @Transactional
    public void clearAndAddToCart(long callerUserNo, CartAddRequestDto dto) {
        verifyOwner(callerUserNo, dto.getUserNo());
        Long storeId = cartMapper.findStoreIdByMenuId(dto.getMenuId());

        cartRepository.findByUserNo(dto.getUserNo()).ifPresent(cart -> {
            cartDetailRepository.deleteByCartId(cart.getCartId());
            cartRepository.delete(cart);
            cartRepository.flush();
        });

        Cart newCart = new Cart();
        newCart.setUserNo(dto.getUserNo());
        newCart.setStoreId(storeId);
        cartRepository.saveAndFlush(newCart);

        CartDetail detail = newDetail(newCart.getCartId(), dto.getMenuId(), dto.getQuantity());
        cartDetailRepository.saveAndFlush(detail);
    }

    @Transactional
    public void updateCartItem(long callerUserNo, Long cartItemId, int quantity) {
        CartDetail item = cartDetailRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("장바구니 아이템 없음"));
        verifyCartItemOwner(callerUserNo, item.getCartId());
        item.setQuantity(quantity);  // dirty checking
    }

    @Transactional
    public void deleteCartItem(long callerUserNo, Long cartItemId) {
        CartDetail item = cartDetailRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("장바구니 아이템 없음"));
        verifyCartItemOwner(callerUserNo, item.getCartId());
        Long cartId = item.getCartId();
        cartDetailRepository.delete(item);
        cartDetailRepository.flush();

        long remain = cartDetailRepository.countByCartId(cartId);
        if (remain == 0) {
            cartRepository.deleteById(cartId);
        }
    }

    @Transactional
    public void clearCart(long callerUserNo, Long userNo) {
        verifyOwner(callerUserNo, userNo);
        cartRepository.findByUserNo(userNo).ifPresent(cart -> {
            cartDetailRepository.deleteByCartId(cart.getCartId());
            cartRepository.delete(cart);
        });
    }

    /** URL/dto의 targetUserNo가 호출자와 일치하는지 동결 검증 (자기 카트만) */
    private void verifyOwner(long callerUserNo, Long targetUserNo) {
        if (!Objects.equals(targetUserNo, callerUserNo)) {
            throw new BusinessException("본인 장바구니만 접근 가능합니다.", HttpStatus.FORBIDDEN);
        }
    }

    /** cartItemId의 cart 소유자가 호출자인지 동결 검증 (cartId → Cart.userNo 조회) */
    private void verifyCartItemOwner(long callerUserNo, Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new BusinessException("장바구니를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(cart.getUserNo(), callerUserNo)) {
            throw new BusinessException("본인 장바구니 아이템만 접근 가능합니다.", HttpStatus.FORBIDDEN);
        }
    }

    private static CartDetail newDetail(Long cartId, Long menuId, int quantity) {
        CartDetail d = new CartDetail();
        d.setCartId(cartId);
        d.setMenuId(menuId);
        d.setQuantity(quantity);
        return d;
    }
}
