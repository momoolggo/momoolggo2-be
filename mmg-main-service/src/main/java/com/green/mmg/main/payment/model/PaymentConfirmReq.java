package com.green.mmg.main.payment.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentConfirmReq {
    private String paymentKey;
    private String orderId;    // 토스에서 문자열로 넘어옴
    private int    amount;
    private int    payState;   // Vue에서 함께 전달
}