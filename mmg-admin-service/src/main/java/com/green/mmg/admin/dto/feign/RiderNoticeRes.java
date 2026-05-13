package com.green.mmg.admin.dto.feign;

import java.time.LocalDateTime;

public record RiderNoticeRes(
        Long noticeNo,
        String title,
        String content,
        String targetType,
        String sendType,
        LocalDateTime reservedAt,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {}