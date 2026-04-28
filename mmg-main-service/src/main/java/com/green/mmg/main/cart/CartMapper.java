package com.green.mmg.main.cart;

import com.green.mmg.main.cart.model.Cart;
import com.green.mmg.main.cart.model.CartAddRequestDto;
import com.green.mmg.main.cart.model.CartItemRes;
import com.green.mmg.main.cart.model.CartListRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {
    Long findStoreIdByMenuId(@Param("menuId") Long menuId);
    String findStoreNameByStoreId(@Param("storeId") Long storeId);
    Long findCartIdByCartItemId(@Param("cartItemId") Long cartItemId);
    int countCartItems(@Param("cartId") Long cartId);
    List<CartItemRes> findCartItems(@Param("cartId") Long cartId);
    Cart findCartEntityByUserNo(@Param("userNo") Long userNo);
    CartListRes findCartWithItems(@Param("userNo") Long userNo);
    void insertCart(@Param("userNo") Long userNo, @Param("storeId") Long storeId);
    Long getLastCartId();
    Long findCartItemId(@Param("cartId") Long cartId, @Param("menuId") Long menuId);
    void addCartItemQuantity(@Param("cartItemId") Long cartItemId, @Param("quantity") int quantity);
    void insertCartItem(@Param("cartId") Long cartId, @Param("menuId") Long menuId, @Param("quantity") int quantity);
    void updateCartItem(@Param("cartItemId") Long cartItemId, @Param("quantity") int quantity);
    void deleteCartItem(@Param("cartItemId") Long cartItemId);
    void deleteAllCartItems(@Param("cartId") Long cartId);
    void deleteCart(@Param("cartId") Long cartId);
}