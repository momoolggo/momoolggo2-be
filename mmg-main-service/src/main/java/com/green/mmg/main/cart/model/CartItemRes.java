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
    private Integer optionPrice;
    private String optionSummary;
    private String optionSignature;
    private Integer quantity;
    private String menuPic;

    public Integer getUnitPrice() {
        return (price == null ? 0 : price) + (optionPrice == null ? 0 : optionPrice);
    }
}
