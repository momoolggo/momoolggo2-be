package com.green.mmg.admin.dto.feign;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class InternalStoreListRes {

    private Long storeId;
    private String storeName;
    private String ownerName;
    private String location;
    private String storeTel;
    private Integer state;
    private String category;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
