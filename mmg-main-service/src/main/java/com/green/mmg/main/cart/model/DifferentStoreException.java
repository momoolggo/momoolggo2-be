package com.green.mmg.main.cart.model;

// 다른 매장 메뉴 담을 때 던지는 커스텀 예외
public class DifferentStoreException extends RuntimeException {
    public DifferentStoreException(String message) {
        super(message);
    }
}