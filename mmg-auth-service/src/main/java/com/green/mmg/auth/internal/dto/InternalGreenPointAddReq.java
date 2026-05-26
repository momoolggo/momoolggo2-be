package com.green.mmg.auth.internal.dto;

public record InternalGreenPointAddReq(
        Integer point,
        String reason,
        Long orderId
) {
}