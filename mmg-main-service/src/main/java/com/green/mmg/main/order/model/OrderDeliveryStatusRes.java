package com.green.mmg.main.order.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderDeliveryStatusRes {
    private Long orderId;
    private Integer orderState;
    private String orderStateText;
    private Integer deliveryState;
    private String deliveryStateText;
}
