package com.green.mmg.main.owner.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerReviewReq {
    private Long storeId;
    private int page = 1;
    private int size = 10;

}
