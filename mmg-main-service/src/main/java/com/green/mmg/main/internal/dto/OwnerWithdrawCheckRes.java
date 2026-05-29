package com.green.mmg.main.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OwnerWithdrawCheckRes {
    private boolean hasActiveStore;
    private boolean hasActiveOrders;
    private List<Long> storeIds;
}
