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
 * <p>setter 미공개 — 상태 전환은 명시 메서드(toggleEating/resumeActive/approve 등)로만 변경 (ADR-004 화이트리스트).
 * 2026-05-28 트랙 — 가입 신원 승인 흐름 복원. 가입 시 status=PENDING 박제, admin이 approve() 호출 시 ACTIVE.
 * (이전: SSE 자동화 트랙(2026-05-21)에서 영구 폐기 박제. 2026-05-28 트랙에서 복원 — 사용자 명시 요청.)</p>
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

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "license_image_url", length = 500)
    private String licenseImageUrl;

    /**
     * 7 파라미터 생성자 — phone/licenseImageUrl 미입력(NULL) 허용. 기존 테스트 호환 박제.
     * 신규 가입 흐름은 9 파라미터 생성자 사용 (joinProfile 박제 일관).
     */
    public Rider(Long userNo, String licenseNo, String licenseType, VehicleType vehicleType,
                 String accountBank, String accountNo, String accountHolder) {
        this(userNo, licenseNo, licenseType, vehicleType,
                accountBank, accountNo, accountHolder, null, null);
    }

    /**
     * 정산 시연 UX 트랙 #9 (2026-05-21, 옵션 A) — phone 스냅샷 박제용 8 파라미터.
     * licenseImageUrl 미입력(NULL) 허용. 기존 호출처 호환 박제.
     */
    public Rider(Long userNo, String licenseNo, String licenseType, VehicleType vehicleType,
                 String accountBank, String accountNo, String accountHolder, String phone) {
        this(userNo, licenseNo, licenseType, vehicleType,
                accountBank, accountNo, accountHolder, phone, null);
    }

    /**
     * 2026-05-28 트랙 — 가입 시 면허증 사진 URL 박제용 9 파라미터.
     * 가입 신원 승인 흐름 복원: status=PENDING 박제, admin이 approve() 호출 시 ACTIVE 전이.
     * (이전: SSE 자동화 트랙(2026-05-21)에서 status=ACTIVE 박제 — 2026-05-28 트랙에서 PENDING으로 복원.)
     */
    public Rider(Long userNo, String licenseNo, String licenseType, VehicleType vehicleType,
                 String accountBank, String accountNo, String accountHolder, String phone, String licenseImageUrl) {
        this.userNo = userNo;
        this.licenseNo = licenseNo;
        this.licenseType = licenseType;
        this.vehicleType = vehicleType;
        this.accountBank = accountBank;
        this.accountNo = accountNo;
        this.accountHolder = accountHolder;
        this.phone = phone;
        this.licenseImageUrl = licenseImageUrl;
        this.status = RiderStatus.PENDING;
    }

    /**
     * PENDING → ACTIVE 승인 (2026-05-28 트랙 복원).
     * ADR-004 화이트리스트 — PENDING 외 상태에서 호출 시 IllegalStateException (Service 진입부 BusinessException 변환).
     */
    public void approve() {
        if (this.status != RiderStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태가 아닌 라이더는 승인할 수 없습니다. 현재 상태=" + this.status);
        }
        this.status = RiderStatus.ACTIVE;
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
