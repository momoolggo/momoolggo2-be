package com.green.mmg.main.cart;

import com.green.mmg.main.cart.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Phase 3-B-3: 하이브리드 영구 공존 핵심 검증 도메인.
 *
 * <p>Repository (JPA, 단순 CRUD): Cart/CartDetail save·find·delete·count + dirty checking.<br>
 * Mapper (MyBatis, 잔존): findStoreIdByMenuId(JOIN), findCartItems(JOIN),
 * findStoreNameByStoreId(도메인 경계 위반이지만 Phase 3-D Store 정리 시 함께 검토).</p>
 *
 * <p>saveAndFlush 패턴: JPA INSERT 후 같은 @Transactional 내에서 MyBatis SELECT가
 * 즉시 보이도록 영속성 컨텍스트 → DB 동기화. Phase 3-B-2 LikedStore 검증과 동일.</p>
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;                  // MyBatis (JOIN 잔존)
    private final CartRepository cartRepository;          // JPA
    private final CartDetailRepository cartDetailRepository;  // JPA

    public CartListRes getCart(Long userNo) {
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
        return response;
    }

    @Transactional
    public void addToCart(CartAddRequestDto dto) {
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
    public void clearAndAddToCart(CartAddRequestDto dto) {
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
    public void updateCartItem(Long cartItemId, int quantity) {
        CartDetail item = cartDetailRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("장바구니 아이템 없음"));
        item.setQuantity(quantity);  // dirty checking
    }

    @Transactional
    public void deleteCartItem(Long cartItemId) {
        CartDetail item = cartDetailRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("장바구니 아이템 없음"));
        Long cartId = item.getCartId();
        cartDetailRepository.delete(item);
        cartDetailRepository.flush();

        long remain = cartDetailRepository.countByCartId(cartId);
        if (remain == 0) {
            cartRepository.deleteById(cartId);
        }
    }

    @Transactional
    public void clearCart(Long userNo) {
        cartRepository.findByUserNo(userNo).ifPresent(cart -> {
            cartDetailRepository.deleteByCartId(cart.getCartId());
            cartRepository.delete(cart);
        });
    }

    private static CartDetail newDetail(Long cartId, Long menuId, int quantity) {
        CartDetail d = new CartDetail();
        d.setCartId(cartId);
        d.setMenuId(menuId);
        d.setQuantity(quantity);
        return d;
    }
}
