package com.green.mmg.main.owner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "menu_option")
public class MenuOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long optionId;

    @Column(name = "option_category_no", nullable = false)
    private long optionCategoryNo;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false, length = 20)
    private String soldOut;

    public MenuOption(long optionCategoryNo, String name, Integer price, String soldOut) {
        this.optionCategoryNo = optionCategoryNo;
        this.name = name;
        this.price = price;
        this.soldOut = soldOut;
    }
    public void update(String name, Integer price, String soldOut) {
        this.name = name;
        this.price = price;
        this.soldOut = soldOut;
    }
}
