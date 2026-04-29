package com.green.mmg.main.payment;

import org.apache.ibatis.annotations.Mapper;

/**
 * Phase 3-B 하이브리드 잔존: existsByOrderId 1개 (orders 테이블 pay_state 검사 — payment 테이블 아님).
 * insertPayment는 PaymentRepository.save()로 전환됨.
 */
@Mapper
public interface PaymentMapper {
    boolean existsByOrderId(Long orderId);
}
