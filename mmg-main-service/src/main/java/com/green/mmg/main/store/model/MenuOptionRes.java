package com.green.mmg.main.store.model;

import com.green.mmg.main.owner.entity.MenuOption;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuOptionRes {
    private long optionId;
    private long optionCategoryNo;
    private String name;
    private Integer price;
    private String soldOut;


    public static MenuOptionRes from(MenuOption entity) {
        MenuOptionRes res = new MenuOptionRes();
        res.setOptionId(entity.getOptionId());
        res.setName(entity.getName());
        res.setPrice(entity.getPrice());
        res.setSoldOut(entity.getSoldOut());
        res.setOptionCategoryNo(entity.getOptionCategoryNo());

        return res;
    }

}
