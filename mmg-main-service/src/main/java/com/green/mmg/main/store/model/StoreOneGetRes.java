package com.green.mmg.main.store.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StoreOneGetRes {
    private String storeName;
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
    private String business_number;
    private String storeTel;
    private int ratingAvg;
    private int ratingCount;
    private int orderCount;
}
