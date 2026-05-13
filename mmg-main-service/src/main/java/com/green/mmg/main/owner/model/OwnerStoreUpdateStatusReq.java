package com.green.mmg.main.owner.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerStoreUpdateStatusReq {
    private long storeId;
    private String businessHours;
    private String holiday;
    private String notice; //가게 공지
    private int minPrice;
    private int state;
}
