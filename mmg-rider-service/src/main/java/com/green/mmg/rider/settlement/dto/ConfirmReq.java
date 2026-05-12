package com.green.mmg.rider.settlement.dto;

/**
 * Internal admin → rider — 정산 confirm 요청 (POST /internal/rider/settlement/{id}/confirm).
 * adminNo는 호출자 admin 식별자 (X-Admin-No 헤더 대안).
 */
public record ConfirmReq(
        Long adminNo
) {
}
