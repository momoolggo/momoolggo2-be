package com.green.mmg.main.order.model;

import com.green.mmg.main.cart.model.CartItemRes;
import lombok.Data;
import java.util.List;

@Data
public class OrderInfoRes {
    private String  storeName;
    private String  tel;
    private String  address;
    private String  addressDetail;
    private List<CartItemRes> items;
    private Integer menuTotal;
    private Integer deliveryFee;
    private Integer totalAmount;
}