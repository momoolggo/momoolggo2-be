package com.green.mmg.main.address.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddressSearchRes {
    private String roadAddress;   // 도로명 주소
    private String jibunAddress;  // 지번 주소
    private double lat;           // 위도
    private double lng;           // 경도
}