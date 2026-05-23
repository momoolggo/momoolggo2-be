package com.green.mmg.admin.cs.dto;

import lombok.Getter;

@Getter
public class EscalationPayload {
    private final Long inquiryId;
    private final Long userNo;
    private final Long sessionId;
    private final String content;

    public EscalationPayload(Long inquiryId, Long userNo, Long sessionId, String content) {
        this.inquiryId = inquiryId;
        this.userNo = userNo;
        this.sessionId = sessionId;
        this.content = content;
    }
}
