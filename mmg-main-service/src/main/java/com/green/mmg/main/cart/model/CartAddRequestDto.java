package com.green.mmg.main.cart.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Data
public class CartAddRequestDto {
    private Long userNo;
    private Long menuId;
    private int quantity;
}