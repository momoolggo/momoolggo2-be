package com.green.mmg.main.owner.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OwnerMenuOptionCategoryRegReq {
    private long menuId;
    private String optionCategoryName;
    private Boolean isRequired;
    private Integer maxSelect;

    private List<OptionItem> options;

    @Getter
    @Setter
    public static class OptionItem{
        private String name;
        private Integer price;
        private String soldOut;
    }
}
