package com.green.mmg.admin.dto.feign;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TodayStatsRes {
    private long newUserCount;
    private long storeCount;
    private long reviewCount;
}