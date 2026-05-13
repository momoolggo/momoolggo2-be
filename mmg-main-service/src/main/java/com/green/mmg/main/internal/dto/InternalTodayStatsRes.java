package com.green.mmg.main.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class InternalTodayStatsRes {
    private long newUserCount;
    private long storeCount;
    private long reviewCount;
}
