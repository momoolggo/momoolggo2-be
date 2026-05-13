package com.green.mmg.main.owner.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerMenuRes { //서버가 주는 최종 메뉴등록 결과
    private Long menuId;
    private String name;
    private Long categoryId;
    private String menuInfo;
    private String price;
    private String menuPic;

}
