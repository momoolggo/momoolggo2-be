package com.green.mmg.main.pet.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 펫 레벨업 시 포인트 보상 outbox.
 * order_id UNIQUE — 1주문 1회 적립 (idempotent).
 * status: PENDING → (별 worker가 auth user.green 동기화 후) SUCCESS / FAILED.
 *
 * <p>본 P-6에서는 INSERT만. 동기화 worker는 Phase 6+ 위임 (tech-debt).</p>
 */
@Entity
@Table(name = "green_point_log")
@Getter
@NoArgsConstructor
public class GreenPointLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "green_point_log_id")
    private Long greenPointLogId;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "point", nullable = false)
    private Integer point;

    @Column(name = "reason", length = 100, nullable = false)
    private String reason;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public GreenPointLog(Long orderId, Long userNo, int point, String reason) {
        this.orderId = orderId;
        this.userNo = userNo;
        this.point = point;
        this.reason = reason;
        this.status = "PENDING";
        this.retryCount = 0;
    }
}
