package com.green.mmg.main.ownerprofile.dto;

import com.green.mmg.main.ownerprofile.OwnerProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OwnerProfileDetailRes {
    private String storeAddress;
    private String businessNumber;
    private String businessLicenseUrl;
    private String mailOrderLicenseUrl;
    private String bankName;
    private String accountNumber;
    private String accountHolder;

    public static OwnerProfileDetailRes from(OwnerProfile profile) {
        return new OwnerProfileDetailRes(
                profile.getStoreAddress(),
                profile.getBusinessNumber(),
                profile.getBusinessLicenseUrl(),
                profile.getMailOrderLicenseUrl(),
                profile.getBankName(),
                profile.getAccountNumber(),
                profile.getAccountHolder()
        );
    }
}