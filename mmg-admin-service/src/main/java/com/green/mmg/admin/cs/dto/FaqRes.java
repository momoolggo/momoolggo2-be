package com.green.mmg.admin.cs.dto;

import com.green.mmg.admin.common.enums.FaqCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FaqRes {
    private Long faqId;
    private FaqCategory type;
    private String question;
    private String answer;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}