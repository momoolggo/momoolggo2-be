package com.green.mmg.admin.cs.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EscalationReq {
    private Long userNo;
    private Long sessionId;
    private String lastUserMessage;
}
