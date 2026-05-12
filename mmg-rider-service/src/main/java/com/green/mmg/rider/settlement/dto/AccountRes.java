package com.green.mmg.rider.settlement.dto;

/**
 * 정산 계좌 조회 응답 — GET /api/rider/settlement/account.
 * Rider entity의 accountBank/accountNo/accountHolder 노출 (본인 한정).
 */
public record AccountRes(
        String accountBank,
        String accountNo,
        String accountHolder
) {
}
