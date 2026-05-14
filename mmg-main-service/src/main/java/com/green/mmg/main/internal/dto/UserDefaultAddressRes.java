package com.green.mmg.main.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class UserDefaultAddressRes {
    private long userNo;
    private String address;
    private String addressDetail;
}
