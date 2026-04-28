package com.green.mmg.main.address.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAddressReq {
    private Long userNo;
    private String address;        // 기본주소
    private String addressDetail;  // 상세주소
    private Double latitude;            // 위도
    private Double longitude;            // 경도
    private Integer defaultAd;     // 기본주소 여부 (1:기본, 0:일반)
}
