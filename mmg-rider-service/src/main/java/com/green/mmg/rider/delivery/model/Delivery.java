package com.green.mmg.rider.delivery.model;

import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * delivery 테이블 엔티티 (my_mmg_rider.delivery).
 *
 * <p>외부 참조 (논리 FK, 물리 FK 제약 X — CLAUDE.md §3 MSA 경계):
 * <ul>
 *   <li>{@code order_id} → my_mmg_main.orders.order_id (Main이 배차 시 Feign으로 전달)</li>
 *   <li>{@code rider_no} → rider.rider_no (NULL = WAITING_ASSIGN)</li>
 * </ul>
 *
 * <p>BaseEntity 상속: created_at / updated_at 컬럼 자동 매핑 (R1-A Rider 패턴 일관, Phase 3-C 검증).</p>
 *
 * <p>setter 미공개 — 상태 전환은 명시 메서드(R3 DeliveryService 진입 시 assign/arriveAtStore/pickup/deliver 등)로만 변경.
 * R2 시점은 entity 형태 + WAITING_ASSIGN 가입 시점 고정 생성자만 도입.</p>
 *
 * <p>{@code @Version} 낙관적 락 (Q5-A, ADR-004 D5) — 동시 변경 시 ConflictException 변환.</p>
 *
 * <p>{@code delivery_no} VARCHAR PK (8자 형식 {@code 00001ABC}) — application 생성 (Figma 정정 9, Phase 3-C Order 패턴).
 * generation 알고리즘은 R3 DeliveryService 진입 시 결정.</p>
 */
@Entity
@Table(name = "delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    @Column(name = "delivery_no", length = 20)
    private String deliveryNo;

    @Column(name = "order_id", length = 20, nullable = false)
    private String orderId;

    @Column(name = "rider_no")
    private Long riderNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private DeliveryStatus status;

    @Column(name = "pickup_phone", length = 20)
    private String pickupPhone;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "pickup_address", length = 200)
    private String pickupAddress;

    @Column(name = "pickup_lat", precision = 16, scale = 13)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double pickupLat;

    @Column(name = "pickup_lng", precision = 16, scale = 13)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double pickupLng;

    @Column(name = "delivery_address", length = 200)
    private String deliveryAddress;

    @Column(name = "delivery_lat", precision = 16, scale = 13)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double deliveryLat;

    @Column(name = "delivery_lng", precision = 16, scale = 13)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double deliveryLng;

    @Column(name = "base_fee", nullable = false)
    private Integer baseFee;

    @Column(name = "extra_fee", nullable = false)
    private Integer extraFee;

    @Column(name = "delivered_method", length = 30)
    private String deliveredMethod;

    @Column(name = "delivered_photo_url", length = 500)
    private String deliveredPhotoUrl;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "arrived_at_store_at")
    private LocalDateTime arrivedAtStoreAt;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "delivering_at")
    private LocalDateTime deliveringAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * 배차 대기 시점 생성자 — Main이 Feign으로 배차 요청 시 호출 (R3 DeliveryService).
     * status 고정: WAITING_ASSIGN (가입 시점 PENDING 패턴 일관, R1-A Rider 일관).
     * extra_fee 고정: 0 (DDL DEFAULT 0 일관).
     */
    public Delivery(String deliveryNo, String orderId,
                    String pickupPhone, String customerPhone,
                    String pickupAddress, Double pickupLat, Double pickupLng,
                    String deliveryAddress, Double deliveryLat, Double deliveryLng,
                    Integer baseFee) {
        this.deliveryNo = deliveryNo;
        this.orderId = orderId;
        this.status = DeliveryStatus.WAITING_ASSIGN;
        this.pickupPhone = pickupPhone;
        this.customerPhone = customerPhone;
        this.pickupAddress = pickupAddress;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.deliveryAddress = deliveryAddress;
        this.deliveryLat = deliveryLat;
        this.deliveryLng = deliveryLng;
        this.baseFee = baseFee;
        this.extraFee = 0;
    }

    // 비즈니스 메서드 (assignRider / arriveAtStore / pickup / deliver / changeStatus 등) — R3 DeliveryService 진입 시 추가.
    // R2 범위는 entity 형태 + 명시 생성자만.
}
