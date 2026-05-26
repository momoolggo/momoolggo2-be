package com.green.mmg.main.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InternalChartStatsRes {
    private String label;
    private long value;
}
