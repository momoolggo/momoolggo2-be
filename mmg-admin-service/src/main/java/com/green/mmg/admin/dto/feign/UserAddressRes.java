package com.green.mmg.admin.dto.feign;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserAddressRes {
    private Long addressId;
    private String address;
    private String addressDetail;
    private Double latitude;
    private Double longitude;
    private Integer defaultAd;
}