package com.green.mmg.main.store.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuGetRes {
    private Long menuId;
    private String categoryName;
    private String menuName;
    private int price;
    private String menuPic;
    private String menuInfo;
    private int soldout;
}
