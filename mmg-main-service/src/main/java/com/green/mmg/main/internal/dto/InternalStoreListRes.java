package com.green.mmg.main.internal.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InternalStoreListRes {
    private Long storeId;
    private String storeName;
    private Long ownerId;
    private String ownerName;
    private String location;
    private String storeTel;
    private LocalDateTime createdAt;
    private Integer state;
    private String categoryName;
}
