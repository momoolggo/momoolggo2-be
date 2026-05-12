package com.green.mmg.rider.settlement.dto;

/**
 * 정산 계좌 변경 요청 — PUT /api/rider/settlement/account.
 * Q-AccountChange (가) 자유 변경 박제 일관 (PENDING 정산 잔존 시에도 변경 허용).
 */
public record AccountReq(
        String accountBank,
        String accountNo,
        String accountHolder
) {
}
