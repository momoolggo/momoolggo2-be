package com.green.mmg.admin.dto.feign;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerProfileRes {
    private String storeAddress;
    private String businessNumber;
    private String businessLicenseUrl;
    private String mailOrderLicenseUrl;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
}