package com.green.mmg.main.owner.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerStoreRes { //가게 운영상태: 서버가 사장님 화면에 현재 설정된 모든 정보를 다 보여주는 용도
    private Long storeId;
    private String storeName;
    private String location;
    private String storeTel;
    private String businessNumber;
    private String storePic;
    private String storeInfo;
    private int state;
    private String holiday;
    private String businessHours;  // 추가
    private int minPrice;          // 추가

}
