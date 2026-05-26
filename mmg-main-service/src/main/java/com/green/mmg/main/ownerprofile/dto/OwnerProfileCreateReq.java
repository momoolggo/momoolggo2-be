package com.green.mmg.main.ownerprofile.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerProfileCreateReq {
    private Long userNo;
    private String storeAddress;
    private String businessNumber;
    private String businessLicenseUrl;
    private String mailOrderLicenseUrl;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
}