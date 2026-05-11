package com.green.mmg.main.owner.model;
import com.green.mmg.main.owner.entity.MenuOption;
import com.green.mmg.main.owner.entity.MenuOptionCategory;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OwnerMenuOptionCategoryRes {
    private long optionCategoryNo;
    private long menuId;
    private String optionCategoryName;
    private Boolean isRequired;
    private Integer maxSelect;
    private List<OwnerMenuOptionRes> options;

    public static OwnerMenuOptionCategoryRes from(MenuOptionCategory category, List<MenuOption> options) {
        OwnerMenuOptionCategoryRes res = new OwnerMenuOptionCategoryRes();
        res.setOptionCategoryNo(category.getOptionCategoryNo());
        res.setMenuId(category.getMenuId());
        res.setOptionCategoryName(category.getOptionCategoryName());
        res.setIsRequired(category.getIsRequired());
        res.setMaxSelect(category.getMaxSelect());
        res.setOptions(
                options.stream()
                        .map(OwnerMenuOptionRes::from)
                        .toList()
        );
        return res;

    }
}
