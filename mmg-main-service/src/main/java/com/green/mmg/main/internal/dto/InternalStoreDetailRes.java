package com.green.mmg.main.internal.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class InternalStoreDetailRes {
    private Long storeNo;
    private String storeName;
    private Long ownerId;
    private String ownerName;
    private String businessNumber;
    private String storeTel;
    private String address;
    private String location;
    private String detailLocation;
    private Integer state;
    private String createdAt;
}
