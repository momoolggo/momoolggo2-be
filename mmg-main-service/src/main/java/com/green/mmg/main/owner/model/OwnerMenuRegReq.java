package com.green.mmg.main.owner.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerMenuRegReq { //메뉴 등록
    private long storeId;
    private String name;
    private String menuInfo;
    private long categoryId;
    private int price;
    private String menuPic;
    private long menuId;
}
