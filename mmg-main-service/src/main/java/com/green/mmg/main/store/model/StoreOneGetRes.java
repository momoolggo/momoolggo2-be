package com.green.mmg.main.store.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StoreOneGetRes {
    private String storeName;
    private Long ownerId;       // Phase 4-A: AuthFeignClient.getOwner() 호출용 (응답에 같이 노출됨)
    private String ownerName;
    private String businessHours;
    private int minPrice;
    private String storePic;
    private String holiday;
    private int state;
    private String location;
    private String detailLocation;
    private String createdAt;
    private String notice;
    private String businessNumber;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String storeTel;
    private int ratingAvg;
    private int ratingCount;
    private int orderCount;
    private String category;
}
