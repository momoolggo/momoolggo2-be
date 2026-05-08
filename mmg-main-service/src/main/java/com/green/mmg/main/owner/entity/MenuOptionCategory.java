package com.green.mmg.main.owner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "menu_option_category")
public class MenuOptionCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long optionCategoryNo;

    @Column(name = "menu_id", nullable = false)
    private long menuId;

    @Column(nullable = false, length = 40)
    private String optionCategoryName;

    @Column(nullable = false)
    private Boolean isRequired;

    @Column(nullable = false)
    private Integer maxSelect;

    public MenuOptionCategory(long menuId, String optionCategoryName, Boolean isRequired, Integer maxSelect) {
        this.menuId = menuId;
        this.optionCategoryName = optionCategoryName;
        this.isRequired = isRequired;
        this.maxSelect = maxSelect;
    }

    public void update(String optionCategoryName, Boolean isRequired, Integer maxSelect){
        this.optionCategoryName = optionCategoryName;
        this.isRequired = isRequired;
        this.maxSelect = maxSelect;
    }
}
