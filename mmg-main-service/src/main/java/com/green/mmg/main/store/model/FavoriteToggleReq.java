package com.green.mmg.main.store.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoriteToggleReq {
    private long userNo;
    private long storeId;
}
