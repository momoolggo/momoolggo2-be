package com.green.mmg.main.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InternalStoreListPageRes {
    private List<InternalStoreListRes> content;
    private long totalCount;
}
