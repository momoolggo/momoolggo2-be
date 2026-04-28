package com.green.mmg.main.owner.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerMenuUpdateReq { //메뉴 수정
    private long menuId;
    private String name;
    private long categoryId;
    private String menuInfo;
    private int price;
    private String menuPic;
}
