package com.green.mmg.main.address.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor   // Phase 3-D-B: JPQL constructor expression (UserAddressRepository.findAllByUserNo)
public class UserAddressRes {
    private Long addressId;
    private String address;
    private String addressDetail;
    private Double latitude;
    private Double longitude;
    private Integer defaultAd;
}
