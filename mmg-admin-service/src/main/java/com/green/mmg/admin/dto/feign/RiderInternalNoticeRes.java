package com.green.mmg.admin.dto.feign;

public record RiderInternalNoticeRes(
        String resultMessage
) {
    public static RiderInternalNoticeRes success() {
        return new RiderInternalNoticeRes("공지 발송 완료");
    }
}