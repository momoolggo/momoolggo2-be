package com.green.mmg.auth.internal.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalUserSuspensionReq {
    private Integer days;
    private String reason;
}
