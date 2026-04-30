package com.green.mmg.admin.penalty.entity;

import com.green.mmg.admin.common.enums.PenaltyLevel;
import com.green.mmg.admin.common.enums.PenaltyTarget;
import com.green.mmg.admin.penalty.dto.PenaltyReq;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "penalty")
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id")
    private Long penaltyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private PenaltyTarget targetType;

    @Column(name = "target_no", nullable = false)
    private Long targetNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 15)
    private PenaltyLevel level;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "start_at")
    private LocalDate startAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", length = 20)
    private PenaltyLevel penaltyType;

    @Column(name = "count")
    private Integer count;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 패널티 부여 생성자
    public Penalty(PenaltyReq req) {
        this.targetType = req.getTargetType();
        this.targetNo = req.getTargetNo();
        this.level = req.getLevel();
        this.reason = req.getReason();
        this.durationDays = req.getDurationDays();
        this.startAt = LocalDate.now();
        this.penaltyType = req.getLevel();
        this.count = 1;
        this.endsAt = req.getDurationDays() != null
                ? LocalDateTime.now().plusDays(req.getDurationDays())
                : null;
        this.createdAt = LocalDateTime.now();
    }

    // 패널티 취소
    public void cancel() {
        this.endsAt = LocalDateTime.now();
    }
}