package com.green.mmg.rider.rider.model;

import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * rider 테이블 엔티티 (my_mmg_rider.rider).
 *
 * <p>외부 참조: {@code user_no} → my_mmg_auth.user.user_no (논리 FK, 물리 FK 제약 X — CLAUDE.md §3 MSA 경계).
 * 데이터 정합성은 application 레벨에서 보장 (가입 흐름 ADR-001 Q1-C: auth user 생성 후 PUT /api/rider/profile).</p>
 *
 * <p>BaseEntity 상속: created_at / updated_at 컬럼 자동 매핑 (Auditing).</p>
 *
 * <p>setter 미공개 — 상태 전환은 명시 메서드(approve/changeStatus 등)로만 변경 (ADR-004 화이트리스트 R3에서 도입).
 * R1 시점은 D11 auto-approve로 PENDING → ACTIVE 1회 전환만 사용.</p>
 */
@Entity
@Table(name = "rider")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rider extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rider_no")
    private Long riderNo;

    @Column(name = "user_no", nullable = false, unique = true)
    private Long userNo;

    @Column(name = "license_no", length = 50, nullable = false)
    private String licenseNo;

    @Column(name = "license_type", length = 20, nullable = false)
    private String licenseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 20, nullable = false)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RiderStatus status;

    @Column(name = "account_bank", length = 50)
    private String accountBank;

    @Column(name = "account_no", length = 50)
    private String accountNo;

    @Column(name = "account_holder", length = 50)
    private String accountHolder;

    /**
     * 생성자 — 가입 시점 PENDING 고정 (D11 auto-approve로 ACTIVE 전환은 Service에서).
     */
    public Rider(Long userNo, String licenseNo, String licenseType, VehicleType vehicleType,
                 String accountBank, String accountNo, String accountHolder) {
        this.userNo = userNo;
        this.licenseNo = licenseNo;
        this.licenseType = licenseType;
        this.vehicleType = vehicleType;
        this.accountBank = accountBank;
        this.accountNo = accountNo;
        this.accountHolder = accountHolder;
        this.status = RiderStatus.PENDING;
    }

    /**
     * D11 auto-approve 임시 처리 — admin-service approve endpoint 도입 후 제거 대상.
     * 관련 ADR: docs/adr/rider/ADR-001-service-boundary.md "임시 운영" 섹션.
     */
    public void approve() {
        this.status = RiderStatus.ACTIVE;
    }
}
