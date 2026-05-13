package com.green.mmg.main.owner.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerMenuOptionUpdateReq {
    private long optionId;
    private String name;
    private Integer price;
    private String soldOut;
}
