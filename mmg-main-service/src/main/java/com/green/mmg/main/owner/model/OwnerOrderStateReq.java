package com.green.mmg.main.owner.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerOrderStateReq { // 주문상태 수정용
    private Long orderId;
    private int orderState;
}
