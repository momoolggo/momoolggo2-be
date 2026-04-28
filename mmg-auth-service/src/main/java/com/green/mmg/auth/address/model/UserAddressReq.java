package com.green.mmg.auth.address.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAddressReq {
    private Long userNo;
    private String address;        // 기본주소
    private String addressDetail;  // 상세주소
    private Double lat;            // 위도
    private Double lng;            // 경도
    private Integer defaultAd;     // 기본주소 여부 (1:기본, 0:일반)
}
