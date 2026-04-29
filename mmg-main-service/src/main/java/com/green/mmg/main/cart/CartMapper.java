package com.green.mmg.main.cart;

import com.green.mmg.main.cart.model.Cart;
import com.green.mmg.main.cart.model.CartItemRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Phase 3-B-3 잔존:
 * <ul>
 *   <li>JOIN 3개 (findStoreIdByMenuId, findCartItems): JPA 부적합 — 영구 잔존</li>
 *   <li>findStoreNameByStoreId: Store 도메인 경계 (Phase 3-D 정리 예정)</li>
 *   <li>findCartEntityByUserNo, deleteAllCartItems, deleteCart: 외부 호출 (OrderService, PaymentService)
 *       — Phase 3-C Order 전환 시 호출 측을 CartService로 위임 후 제거</li>
 * </ul>
 *
 * <p>CartService 자체는 11개 단순 CRUD를 모두 CartRepository / CartDetailRepository로 이전.
 * getLastCartId는 JPA save() 후 entity.cartId 자동 채움으로 완전 대체 (decisions.md 보존 정책 예외).</p>
 */
@Mapper
public interface CartMapper {

    /** menu → menu_category JOIN */
    Long findStoreIdByMenuId(@Param("menuId") Long menuId);

    /** Store 도메인 경계 위반 — Phase 3-D 정리 예정 */
    String findStoreNameByStoreId(@Param("storeId") Long storeId);

    /** cart_detail → menu JOIN, 응답 DTO 직접 매핑 */
    List<CartItemRes> findCartItems(@Param("cartId") Long cartId);

    /** 외부 호출 (OrderService, PaymentService) — Phase 3-C에서 CartService 위임으로 제거 예정 */
    Cart findCartEntityByUserNo(@Param("userNo") Long userNo);

    /** 외부 호출 (PaymentService confirmPayment) — Phase 3-C 정리 예정 */
    void deleteAllCartItems(@Param("cartId") Long cartId);

    /** 외부 호출 (PaymentService confirmPayment) — Phase 3-C 정리 예정 */
    void deleteCart(@Param("cartId") Long cartId);
}
