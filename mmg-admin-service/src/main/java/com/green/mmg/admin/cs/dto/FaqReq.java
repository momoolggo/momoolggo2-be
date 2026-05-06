package com.green.mmg.admin.cs.dto;

import com.green.mmg.admin.common.enums.FaqCategory;
import lombok.Getter;

@Getter
public class FaqReq {
    private FaqCategory type;
    private String question;
    private String answer;
}