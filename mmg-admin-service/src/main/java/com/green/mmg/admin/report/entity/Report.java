package com.green.mmg.admin.report.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "reporter_no", nullable = false)
    private Long reporterNo;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_no", nullable = false)
    private Long targetNo;

    @Column(name = "reason", nullable = false, length = 50)
    private String reason;

    @Column(name = "content", length = 500)
    private String content;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // AI 관련 필드
    @Column(name = "review_content", columnDefinition = "TEXT")
    private String reviewContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", nullable = false, length = 20)
    private AiStatus aiStatus = AiStatus.PENDING;

    @Column(name = "ai_should_blind")
    private Boolean aiShouldBlind;

    @Column(name = "ai_reason", length = 100)
    private String aiReason;

    @Column(name = "ai_violations", length = 300)
    private String aiViolations;

    @Column(name = "ai_confidence", length = 10)
    private String aiConfidence;

    @Column(name = "ai_processed_at")
    private LocalDateTime aiProcessedAt;

    @Column(name = "ai_fail_reason", length = 500)
    private String aiFailReason;

    @Column(name = "ai_retry_count", nullable = false)
    private int aiRetryCount = 0;

    @Column(name = "auto_blinded", nullable = false)
    private boolean autoBlinded = false;

    @Column(name = "auto_blinded_at")
    private LocalDateTime autoBlindedAt;

    @Column(name = "blind_fail_reason", length = 500)
    private String blindFailReason;

    public Report(Long reporterNo, Long targetNo, String reason, String content) {
        this.reporterNo = reporterNo;
        this.targetNo = targetNo;
        this.targetType = "리뷰";
        this.reason = reason;
        this.content = content;
        this.status = "PENDING";
        this.aiStatus = AiStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void applyAiResult(Boolean shouldBlind, String aiReason, String violations,
                               String confidence) {
        this.aiShouldBlind = shouldBlind;
        this.aiReason = aiReason;
        this.aiViolations = violations;
        this.aiConfidence = confidence;
        this.aiStatus = AiStatus.DONE;
        this.aiProcessedAt = LocalDateTime.now();
    }

    public void markAiFailed(String failReason) {
        this.aiStatus = AiStatus.FAILED;
        this.aiFailReason = failReason;
        this.aiRetryCount++;
    }

    public void markAutoBlinded() {
        this.autoBlinded = true;
        this.autoBlindedAt = LocalDateTime.now();
        this.status = "AUTO_BLINDED";
    }

    public void markBlindFailed(String reason) {
        this.blindFailReason = reason;
    }

    public void setReviewContent(String reviewContent) {
        this.reviewContent = reviewContent;
    }
}
