package com.green.mmg.auth.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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