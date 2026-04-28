package com.green.mmg.main.cart.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRes {
    private Long id;// cart_detail.cart_item_id
    private String menuName;
    private long menuId;
    private Integer price;
    private Integer quantity;
    private String menuPic;
}
