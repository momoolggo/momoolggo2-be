package com.green.mmg.rider.rider.model;

import com.green.mmg.common.entity.BaseEntity;
import com.green.mmg.common.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

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
     * admin 승인 처리 (interfaces.md §3.1, Q-A1 (라+) + Q-A20 (가) Group 8.5 정정 2026-05-17).
     *
     * <p>전이 검증: PENDING → ACTIVE만 허용. 다른 상태(ACTIVE/EATING/SUSPENDED)에서 호출 시 BAD_REQUEST.
     * interfaces.md §3.1 박제 "rider 이미 ACTIVE — 400 BAD_REQUEST" 일관.
     * ADR-004 ALLOWED_TRANSITIONS 패턴 일관 (전이 규칙 entity 박제).</p>
     *
     * <p>D11 auto-approve 시점에도 본 메서드 호출 — joinProfile 직후 PENDING → ACTIVE 안전.
     * Q-A17 (iii) toggle 운영 false + dev backup 박제 일관.</p>
     */
    public void approve() {
        if (this.status != RiderStatus.PENDING) {
            throw new BusinessException(
                    "PENDING 상태만 승인 가능합니다 (현재: " + this.status + ").",
                    HttpStatus.BAD_REQUEST);
        }
        this.status = RiderStatus.ACTIVE;
    }

    /**
     * admin 제재 처리 (interfaces.md §3.2, Q-A1 (라+) + Q-A20 (가) Group 8.5 신설 2026-05-17).
     *
     * <p>전이 검증: PENDING/ACTIVE/EATING → SUSPENDED 허용. SUSPENDED → SUSPENDED 중복 시 BAD_REQUEST.
     * interfaces.md §3.2 박제 일관.</p>
     *
     * <p>{@code reason}은 entity 박제 X — audit log/blind 테이블 별 영역 (Q-A18 (b) cross-schema 정합성
     * Phase 6+ outbox 위임 일관). 본 메서드는 단순 status 전이.</p>
     */
    public void suspend() {
        if (this.status == RiderStatus.SUSPENDED) {
            throw new BusinessException(
                    "이미 SUSPENDED 상태입니다.",
                    HttpStatus.BAD_REQUEST);
        }
        this.status = RiderStatus.SUSPENDED;
    }

    /**
     * ACTIVE → EATING 토글 (R8 D8-a, 식사중 진입).
     * 화이트리스트 검증은 WorkSessionService에서 수행 (R3 DeliveryService.ALLOWED_TRANSITIONS 패턴 일관).
     */
    public void toggleEating() {
        this.status = RiderStatus.EATING;
    }

    /**
     * EATING → ACTIVE 복귀 (R8, 식사 종료).
     * 화이트리스트 검증은 WorkSessionService에서 수행.
     */
    public void resumeActive() {
        this.status = RiderStatus.ACTIVE;
    }

    /**
     * 정산 계좌 변경 (R7, PUT /api/rider/settlement/account). Q-AccountChange (가) 자유 변경.
     * 입력 검증(null/blank)은 SettlementService에서 수행.
     */
    public void updateAccount(String accountBank, String accountNo, String accountHolder) {
        this.accountBank = accountBank;
        this.accountNo = accountNo;
        this.accountHolder = accountHolder;
    }
}
