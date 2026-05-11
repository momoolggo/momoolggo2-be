package com.green.mmg.main.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InternalDailyStatsRes {
    private long totalOrders;
    private long newUsers;
    private long totalRevenue;
}