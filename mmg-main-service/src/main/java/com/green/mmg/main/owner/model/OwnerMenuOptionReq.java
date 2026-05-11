package com.green.mmg.main.owner.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerMenuOptionReq { // 메뉴 옵션 등록
    private long optionCategoryNo;
    private String name;
    private Integer price;
    private String soldOut;
}
