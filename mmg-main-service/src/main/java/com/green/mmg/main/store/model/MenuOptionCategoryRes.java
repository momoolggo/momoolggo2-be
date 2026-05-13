package com.green.mmg.main.store.model;

import com.green.mmg.main.owner.entity.MenuOption;
import com.green.mmg.main.owner.entity.MenuOptionCategory;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MenuOptionCategoryRes {
    private long optionCategoryNo;
    private long menuId;
    private String optionCategoryName;
    private Boolean isRequired;
    private Integer maxSelect;
    private List<MenuOptionRes> options;

    public static MenuOptionCategoryRes from(MenuOptionCategory category, List<MenuOption> options) {
        MenuOptionCategoryRes res = new MenuOptionCategoryRes();
        res.setOptionCategoryNo(category.getOptionCategoryNo());
        res.setMenuId(category.getMenuId());
        res.setOptionCategoryName(category.getOptionCategoryName());
        res.setIsRequired(category.getIsRequired());
        res.setMaxSelect(category.getMaxSelect());
        res.setOptions(
                options.stream()
                        .map(MenuOptionRes::from)
                        .toList()
        );
        return res;

    }

}
