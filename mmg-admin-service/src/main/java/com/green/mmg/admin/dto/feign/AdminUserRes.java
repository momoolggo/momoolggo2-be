package com.green.mmg.admin.dto.feign;

import java.util.Date;

public record AdminUserRes(
        Long userNo,
        String userId,
        String name,
        String tel,
        String role,
        String status,
        Date createdAt,
        Integer green
) {}