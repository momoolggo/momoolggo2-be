package com.green.mmg.main.owner.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerMenuOptionCategoryUpdateReq {
    private long optionCategoryNo;
    private String optionCategoryName;
    private Boolean isRequired;
    private Integer maxSelect;
}
