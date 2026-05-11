package com.green.mmg.main.feign.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiderAssignReq {
    private Long orderId;
    private Long storeId;
    private Double storeLat;
    private Double storeLng;
    private String deliveryAddress;
    private Double deliveryLat;
    private Double deliveryLng;
}