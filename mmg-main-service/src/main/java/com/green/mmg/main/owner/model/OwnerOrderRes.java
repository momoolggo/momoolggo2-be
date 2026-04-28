package com.green.mmg.main.owner.model;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OwnerOrderRes {
    private Long orderId;
    private Long userNo;        // customerName -> userNo 로 변경
    private String orderDate;
    private String menuList;
    private int totalPrice;
    private String state;       // orderState(int) -> state(String) 로 변경 (DB가 PENDING, WATING 문자열이라서)
    private String address;
    private String request;
    private String customerName;
    private String tel;
}