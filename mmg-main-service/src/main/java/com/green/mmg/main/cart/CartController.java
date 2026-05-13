package com.green.mmg.main.cart;

import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.cart.model.CartAddRequestDto;
import com.green.mmg.main.cart.model.CartListRes;
import com.green.mmg.main.cart.model.DifferentStoreException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 장바구니 조회
    @GetMapping("/{userNo}")
    public ResponseEntity<?> getCart(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long userNo) {
        CartListRes cart = cartService.getCart(principal.getSignedUserNo(), userNo);
        return ResponseEntity.ok(Map.of("resultData", cart != null ? cart : Map.of()));
    }

    // 장바구니 담기
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestBody CartAddRequestDto dto) {
        try {
            cartService.addToCart(principal.getSignedUserNo(), dto);
            return ResponseEntity.ok(Map.of("result", "success"));
        } catch (DifferentStoreException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "result", "differentStore",
                    "message", e.getMessage()
            ));
        }
    }

    // 장바구니 비우고 새로 담기
    @PostMapping("/clear-and-add")
    public ResponseEntity<?> clearAndAdd(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestBody CartAddRequestDto dto) {
        cartService.clearAndAddToCart(principal.getSignedUserNo(), dto);
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    // 수량 변경
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<?> updateCartItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Integer> body) {
        cartService.updateCartItem(principal.getSignedUserNo(), cartItemId, body.get("quantity"));
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    // 단일 아이템 삭제
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> deleteCartItem(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long cartItemId) {
        cartService.deleteCartItem(principal.getSignedUserNo(), cartItemId);
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    // 장바구니 비우기
    @DeleteMapping("/clear/{userNo}")
    public ResponseEntity<?> clearCart(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long userNo) {
        cartService.clearCart(principal.getSignedUserNo(), userNo);
        return ResponseEntity.ok(Map.of("result", "success"));
    }
}
