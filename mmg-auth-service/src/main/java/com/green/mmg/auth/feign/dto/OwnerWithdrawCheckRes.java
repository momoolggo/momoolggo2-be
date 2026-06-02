package com.green.mmg.auth.feign.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OwnerWithdrawCheckRes {
    private boolean hasActiveStore;
    private boolean hasActiveOrders;
    private List<Long> storeIds;

    public boolean isHasActiveStore() {
        return hasActiveStore;
    }

    public boolean isHasActiveOrders() {
        return hasActiveOrders;
    }
}
