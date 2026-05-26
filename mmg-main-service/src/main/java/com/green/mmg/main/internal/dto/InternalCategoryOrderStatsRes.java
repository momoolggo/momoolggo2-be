package com.green.mmg.main.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InternalCategoryOrderStatsRes {
    private String categoryName;
    private long count;
}
