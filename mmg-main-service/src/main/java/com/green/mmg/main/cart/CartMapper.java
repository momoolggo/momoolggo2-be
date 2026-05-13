package com.green.mmg.main.cart;

import com.green.mmg.main.cart.model.CartItemRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Phase 3-C-3 정리 후 잔존 (3 SQL):
 * <ul>
 *   <li>findStoreIdByMenuId: menu → menu_category JOIN (영구 잔존)</li>
 *   <li>findStoreNameByStoreId: Store 도메인 경계 (Phase 3-D 정리 예정)</li>
 *   <li>findCartItems: cart_detail → menu JOIN, 응답 DTO 직접 매핑 (영구 잔존)</li>
 * </ul>
 *
 * <p>Phase 3-B-3 잔존 외부 호출 3개(findCartEntityByUserNo, deleteAllCartItems, deleteCart)는
 * Phase 3-C-3에서 PaymentService/OrderService를 CartRepository/CartDetailRepository로 위임한 뒤 제거.</p>
 */
@Mapper
public interface CartMapper {

    Long findStoreIdByMenuId(@Param("menuId") Long menuId);

    String findStoreNameByStoreId(@Param("storeId") Long storeId);

    List<CartItemRes> findCartItems(@Param("cartId") Long cartId);
}
