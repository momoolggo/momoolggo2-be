package com.green.mmg.admin.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PolicyRes {
    private Long policyId;
    private String type;
    private Integer version;
    private String title;
    private String content;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}