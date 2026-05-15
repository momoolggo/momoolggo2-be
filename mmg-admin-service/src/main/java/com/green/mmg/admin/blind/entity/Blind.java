package com.green.mmg.admin.blind.entity;

import com.green.mmg.admin.common.enums.BlindReason;
import com.green.mmg.admin.common.enums.BlindStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "blind")
public class Blind {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blind_id")
    private Long blindId;

    @Column(name = "review_no", nullable = false)
    private Long reviewNo;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private BlindReason reason;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays = 7; // 기본 일주일

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BlindStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "content")
    private String content;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "writer")  // 작성자명
    private String writer;

    @Column(name = "extra_description", length = 500)
    private String extraDescription;

    public Blind(Long reviewNo, Long userNo, BlindReason reason,
                 String storeName, String content, Double rating,
                 String writer) {
        this.reviewNo = reviewNo;
        this.userNo = userNo;
        this.reason = reason;
        this.storeName = storeName;
        this.content = content;
        this.rating = rating;
        this.writer = writer;
        this.durationDays = 7;
        this.startAt = LocalDateTime.now();
        this.endsAt = LocalDateTime.now().plusDays(7);
        this.status = BlindStatus.BLINDED;
        this.createdAt = LocalDateTime.now();

    }

    // 신고 접수용 생성자 (REVIEWING 상태)
    public Blind(Long reviewNo, Long userNo, BlindReason reason) {
        this.reviewNo = reviewNo;
        this.userNo = userNo;
        this.reason = reason;
        this.durationDays = 7;
        this.startAt = LocalDateTime.now();
        this.endsAt = LocalDateTime.now().plusDays(7);
        this.status = BlindStatus.REVIEWING;
        this.createdAt = LocalDateTime.now();
    }

    // 블라인드 확정 (REVIEWING → BLINDED)
    public void confirm() {
        this.status = BlindStatus.BLINDED;
        this.startAt = LocalDateTime.now();
        this.endsAt = LocalDateTime.now().plusDays(7);
    }

    // 블라인드 자동 해제
    public void release() {
        this.status = BlindStatus.RELEASED;
    }

    // 계정 15일 정지
    public void suspend() {
        this.status = BlindStatus.SUSPENDED;
        this.endsAt = LocalDateTime.now().plusDays(15);
    }
    // 영구 정지
    public void permanentSuspend() {
        this.status = BlindStatus.PERMANENT;
    }
}