package com.green.mmg.admin.blind.dto;

import com.green.mmg.admin.common.enums.BlindReason;
import lombok.Getter;

@Getter
public class BlindReq {
    private Long reviewNo;
    private Long userNo;
    private BlindReason reason;
    private String storeName;
    private String content;
    private Double rating;
    private String writer;
}