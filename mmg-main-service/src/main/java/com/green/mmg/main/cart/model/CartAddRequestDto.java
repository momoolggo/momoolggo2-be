package com.green.mmg.main.cart.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

@Data
public class CartAddRequestDto {
    private Long userNo;
    private Long menuId;
    private int quantity;
    private List<CartOptionRequestDto> selectedOptions;

    @Getter
    @Setter
    public static class CartOptionRequestDto {
        private Long optionCategoryNo;
        private Long optionId;
    }
}
