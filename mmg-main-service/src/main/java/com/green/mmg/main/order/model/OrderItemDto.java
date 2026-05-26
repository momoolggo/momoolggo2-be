package com.green.mmg.main.order.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor   // JPQL constructor expression (OrderDetailRepository.findItemsByOrderId)
public class OrderItemDto {
    private String name;       // order_detail.menu_name
    private int count;         // order_detail.quantity
    private Integer price;     // order_detail.menu_price
}