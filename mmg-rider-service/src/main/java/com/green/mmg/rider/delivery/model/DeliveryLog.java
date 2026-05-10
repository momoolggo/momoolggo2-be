package com.green.mmg.rider.delivery.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * delivery_log 테이블 엔티티 (my_mmg_rider.delivery_log).
 *
 * <p>이력 본질: INSERT 후 변경 0 (UPDATE/DELETE 미사용). BaseEntity 미상속 — created_at/updated_at 의미 0,
 * changed_at 단일 박제로 충분 (R2-a Delivery 패턴 의도적 비일관, 본질 차이 — Q-R2b-BaseEntity (a)).</p>
 *
 * <p>외부 참조: {@code delivery_no} → delivery.delivery_no (논리 FK, 물리 FK 제약 X — CLAUDE.md §3 MSA 경계).</p>
 *
 * <p>자동 INSERT: ADR-004 line 116-120 — DeliveryService.updateStatus 내부에서 같은 {@code @Transactional} 트랜잭션 안에 INSERT.
 * 실패 시 둘 다 롤백 (원자성).</p>
 *
 * <p>비즈니스 메서드 0 — 이력 = 변경 0. R3 진입 시 메서드 추가 X (DeliveryService가 직접 INSERT).</p>
 */
@Entity
@Table(name = "delivery_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_no")
    private Long logNo;

    @Column(name = "delivery_no", length = 20, nullable = false)
    private String deliveryNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private DeliveryStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30, nullable = false)
    private DeliveryStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", length = 20, nullable = false)
    private ActorRole actorRole;

    @Column(name = "actor_user_no")
    private Long actorUserNo;

    @Column(name = "changed_at", insertable = false, updatable = false,
            nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime changedAt;

    /**
     * 이력 INSERT 시점 생성자 — DeliveryService.updateStatus 내부 호출 (R3 진입 시).
     * fromStatus nullable (최초 INSERT 시 null), actorUserNo nullable (SYSTEM 시 null).
     */
    public DeliveryLog(String deliveryNo, DeliveryStatus fromStatus,
                       DeliveryStatus toStatus, ActorRole actorRole, Long actorUserNo) {
        this.deliveryNo = deliveryNo;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorRole = actorRole;
        this.actorUserNo = actorUserNo;
    }
}
