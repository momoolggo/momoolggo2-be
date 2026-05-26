package com.green.mmg.admin.dto.feign;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class InternalStoreListPageRes {
    private List<InternalStoreListRes> content;
    private long totalCount;
}