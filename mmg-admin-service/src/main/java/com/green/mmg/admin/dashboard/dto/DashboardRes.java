package com.green.mmg.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardRes {
    private long memberCount;
    private long storeCount;
    private long reviewCount;
}