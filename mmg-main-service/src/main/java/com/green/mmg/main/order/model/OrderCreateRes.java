package com.green.mmg.main.order.model;

public record OrderCreateRes(
        Long orderId,
        Integer totalAmount
) {
}