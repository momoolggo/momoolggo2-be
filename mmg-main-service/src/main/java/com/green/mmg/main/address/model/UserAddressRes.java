package com.green.mmg.main.address.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAddressRes {
    private Long addressId;
    private String address;
    private String addressDetail;
    private Double latitude;
    private Double longitude;
    private Integer defaultAd;
}
