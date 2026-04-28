package com.green.mmg.auth.address.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAddressRes {
    private Long addressId;
    private String address;
    private String addressDetail;
    private Double lat;
    private Double lng;
    private Integer defaultAd;
}
