package com.green.mmg.main.owner.model;

import com.green.mmg.main.owner.entity.MenuOption;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerMenuOptionRes {
    private long optionId;
    private long optionCategoryNo;
    private String name;
    private Integer price;
    private String soldOut;


    public static OwnerMenuOptionRes from(MenuOption entity) {
        OwnerMenuOptionRes res = new OwnerMenuOptionRes();
        res.setOptionId(entity.getOptionId());
        res.setName(entity.getName());
        res.setPrice(entity.getPrice());
        res.setSoldOut(entity.getSoldOut());
        res.setOptionCategoryNo(entity.getOptionCategoryNo());

        return res;
    }
}
