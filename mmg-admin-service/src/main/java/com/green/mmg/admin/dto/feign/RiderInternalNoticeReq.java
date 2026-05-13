package com.green.mmg.admin.dto.feign;

import java.time.LocalDateTime;

public record RiderInternalNoticeReq(
        String title,
        String targetType,   // ALL, SPECIFIC
        String content,
        String sendType,     // NOW, RESERVED
        LocalDateTime reservedAt
) {}