package com.green.mmg.admin.notice.dto;

import com.green.mmg.admin.common.enums.NoticeSendType;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class NoticeReq {
    private String title;
    private String content;
    private String target;
    private String regionFilter;
    private NoticeSendType sendType;
    private LocalDateTime sendAt;
}