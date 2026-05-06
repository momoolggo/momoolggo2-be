package com.green.mmg.admin.notice.dto;

import com.green.mmg.admin.common.enums.NoticeSendType;
import lombok.Getter;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NoticeRes {
    private Long noticeId;
    private String title;
    private String content;
    private String target;
    private String regionFilter;
    private NoticeSendType sendType;
    private LocalDateTime sendAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}