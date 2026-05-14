package com.green.mmg.admin.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserApprovalReq {
    private String status;  // ACTIVE | REJECTED
    private String reason;
}