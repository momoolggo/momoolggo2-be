package com.green.mmg.main.owner.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerStoreUpdateReq {
    private String storeId;
    private String storeName;
    private String location;
    private String storeTel;
    private String businessNumber;
    private String storePic;
    private String storeInfo; //가게 소개글

}
