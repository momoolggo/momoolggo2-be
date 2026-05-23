package com.green.mmg.main.order.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// OrderHistoryDto.java
@Getter
@Setter
public class OrderHistoryDto {
    private String date;           // 포맷된 날짜 "3월 15일(일)"
    private String storeName;
    private String storeImage;
    private long storeId;
    private int totalPrice;        // orders.amount
    private int deliveryFee;       // orders.delivery_fee
    private long orderId;          // orders.order_id
    private int orderState;        // orders.order_state
    private int hasReview;
    private List<OrderItemDto> items;
    // 자잘 에러 트랙 #9-B (2026-05-23) — 배달 완료 사진 URL (orderState=6 시 노출)
    private String deliveredPhotoUrl;
}