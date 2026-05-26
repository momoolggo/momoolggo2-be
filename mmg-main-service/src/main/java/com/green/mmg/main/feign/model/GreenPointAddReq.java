package com.green.mmg.main.feign.model;


public record GreenPointAddReq (
        Integer point,
        String reason,
        Long orderId
) {

}

