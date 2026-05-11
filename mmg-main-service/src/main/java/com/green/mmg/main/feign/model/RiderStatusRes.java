package com.green.mmg.main.feign.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiderStatusRes {
    private Long riderNo;
    private String status;
    private Long currentOrderId;
}