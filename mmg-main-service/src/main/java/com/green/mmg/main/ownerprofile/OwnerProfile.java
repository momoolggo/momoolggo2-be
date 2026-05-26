package com.green.mmg.main.ownerprofile;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "owner_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OwnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "owner_profile_id")
    private Long ownerProfileId;

    @Column(name = "user_no", nullable = false, unique = true)
    private Long userNo;

    @Column(name = "store_address", nullable = false, length = 255)
    private String storeAddress;

    @Column(name = "business_number", nullable = false, length = 30)
    private String businessNumber;

    @Column(name = "business_license_url", nullable = false, length = 500)
    private String businessLicenseUrl;

    @Column(name = "mail_order_license_url", nullable = false, length = 500)
    private String mailOrderLicenseUrl;

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 50)
    private String accountHolder;

    public OwnerProfile(Long userNo, String storeAddress, String businessNumber,
                        String businessLicenseUrl, String mailOrderLicenseUrl,
                        String bankName, String accountNumber, String accountHolder) {
        this.userNo = userNo;
        this.storeAddress = storeAddress;
        this.businessNumber = businessNumber;
        this.businessLicenseUrl = businessLicenseUrl;
        this.mailOrderLicenseUrl = mailOrderLicenseUrl;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }
}