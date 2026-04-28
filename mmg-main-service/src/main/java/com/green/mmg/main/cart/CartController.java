package com.green.mmg.main.cart;

import com.green.mmg.main.cart.model.CartAddRequestDto;
import com.green.mmg.main.cart.model.CartListRes;
import com.green.mmg.main.cart.model.DifferentStoreException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 장바구니 조회
    @GetMapping("/{userNo}")
    public ResponseEntity<?> getCart(@PathVariable Long userNo) {
        CartListRes cart = cartService.getCart(userNo);
        return ResponseEntity.ok(Map.of("resultData", cart != null ? cart : Map.of()));
    }

    // 장바구니 담기
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody CartAddRequestDto dto) {
        try {
            cartService.addToCart(dto);
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
    public ResponseEntity<?> clearAndAdd(@RequestBody CartAddRequestDto dto) {
        cartService.clearAndAddToCart(dto);
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    // 수량 변경
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<?> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Integer> body) {
        cartService.updateCartItem(cartItemId, body.get("quantity"));
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    // 단일 아이템 삭제
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> deleteCartItem(@PathVariable Long cartItemId) {
        cartService.deleteCartItem(cartItemId);
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    // 장바구니 비우기
    @DeleteMapping("/clear/{userNo}")
    public ResponseEntity<?> clearCart(@PathVariable Long userNo) {
        cartService.clearCart(userNo);
        return ResponseEntity.ok(Map.of("result", "success"));
    }
}