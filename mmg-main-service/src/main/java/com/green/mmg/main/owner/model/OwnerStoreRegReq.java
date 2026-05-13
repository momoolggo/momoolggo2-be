package com.green.mmg.main.owner.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerStoreRegReq {
    private long userId;        // XML: #{userId}
    private String storeName;   // XML: #{storeName}
    private String businessNumber; // XML: #{businessNumber}
    private String businessName;   // XML: #{businessName}
    private String location;    // XML: #{location}
    private String storePic;    // XML: #{storePic}
    private String storeTel;    // XML: #{storeTel}
    private String storeInfo;   // XML: #{storeInfo}
    private Double lat;
    private Double lng;
    private long categoryId;
    private String addressDetail;
    // 반드시 @Getter 어노테이션이 있거나, getter 메서드가 존재해야 합니다!
}
