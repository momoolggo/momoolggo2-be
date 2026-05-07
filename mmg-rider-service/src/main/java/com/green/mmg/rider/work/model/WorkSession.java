package com.green.mmg.rider.work.model;

import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * work_session 테이블 엔티티 (my_mmg_rider.work_session).
 *
 * <p>외부 참조: {@code rider_no} → rider.rider_no (논리 FK, 물리 FK 제약 X — CLAUDE.md §3 MSA 경계).</p>
 *
 * <p>BaseEntity 상속: created_at / updated_at 컬럼 자동 매핑 (R1-A Rider / R2-a Delivery 패턴 일관, Phase 3-C 검증).
 * UPDATE 다수 도메인 (work_seconds 누적, ended_at 기록) — DeliveryLog (이력 본질) 패턴과 의도적 분리.</p>
 *
 * <p>setter 미공개 — 명시 메서드(addWork / addBreak / end 등)는 R3 WorkSessionService 진입 시 추가.
 * R2 시점은 entity 형태 + 세션 시작 시점 고정 생성자만 도입.</p>
 *
 * <p>관련 ADR: ADR-002 line 141-156 (정정 6) + ADR-008 (Phase 5-R8 본격 구현).
 * 인덱스 1건: rider_no (Q-R2a2 (나) 자동 적용).</p>
 */
@Entity
@Table(name = "work_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_no")
    private Long sessionNo;

    @Column(name = "rider_no", nullable = false)
    private Long riderNo;

    @Column(name = "vehicle_type", length = 20, nullable = false)
    private String vehicleType;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "work_seconds", nullable = false)
    private Integer workSeconds;

    @Column(name = "break_seconds", nullable = false)
    private Integer breakSeconds;

    /**
     * 세션 시작 시점 생성자 — ACTIVE 진입 시 호출 (R3 WorkSessionService).
     * ended_at null (진행 중), work_seconds/break_seconds 0 (DDL DEFAULT 일관, R2-a extra_fee 패턴).
     */
    public WorkSession(Long riderNo, String vehicleType, LocalDateTime startedAt) {
        this.riderNo = riderNo;
        this.vehicleType = vehicleType;
        this.startedAt = startedAt;
        this.workSeconds = 0;
        this.breakSeconds = 0;
    }

    // 비즈니스 메서드 (addWork / addBreak / end) — R3 WorkSessionService 진입 시 추가.
    // R2 범위는 entity 형태 + 명시 생성자만.
}
