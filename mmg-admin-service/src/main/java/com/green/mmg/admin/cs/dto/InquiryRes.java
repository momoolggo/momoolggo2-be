package com.green.mmg.admin.cs.dto;

import com.green.mmg.admin.common.enums.InquiryStatus;
import com.green.mmg.admin.common.enums.InquiryUserType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@AllArgsConstructor
public class InquiryRes {
    private Long inquiryId;
    private Long userNo;
    private InquiryUserType category;
    private String content;
    private InquiryStatus state;
    private String answer;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;

    // 포맷팅 메서드
    public String getInquiryCode() {
        String date = createdAt.format(
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("INQ-%s-%03d", date, inquiryId);
    }
}