package com.green.mmg.main.cart.model;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class Cart {
    private Long cartId;
    private Long userNo;
    private Long storeId;
}