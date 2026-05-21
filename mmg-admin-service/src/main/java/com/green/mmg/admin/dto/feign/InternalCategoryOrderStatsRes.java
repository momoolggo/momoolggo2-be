package com.green.mmg.admin.dto.feign;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InternalCategoryOrderStatsRes {
    private String categoryName;
    private long count;
}
