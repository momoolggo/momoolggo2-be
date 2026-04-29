package com.green.mmg.main.payment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * payment 테이블 엔티티 (my_mmg_main.payment).
 *
 * <p>Phase 3-B: insert는 JPA save()로 전환. existsByOrderId는 PaymentMapper에 잔존
 * (해당 SQL은 orders 테이블 pay_state 검사 — 의미상 Order 도메인이지만 호출 위치 유지).</p>
 *
 * <p>orderId 타입: 기존 String → Long 변경 (DB BIGINT). 응답 노출 없음 (PaymentController는 Map.of 반환).</p>
 *
 * <p>paymentTime: DB DEFAULT current_timestamp() — JPA에선 insertable=false, updatable=false로 위임.</p>
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private long paymentId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "payment_key", length = 200, nullable = false)
    private String paymentKey;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "pay_state", nullable = false)
    private int payState;

    @Column(name = "payment_time", insertable = false, updatable = false)
    private LocalDateTime paymentTime;
}
