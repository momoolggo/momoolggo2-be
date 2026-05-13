package com.green.mmg.admin.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSuspensionReq {
    private Integer days;   // 0이면 영구정지
    private String reason;
}