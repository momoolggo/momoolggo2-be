package com.green.mmg.admin.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ai_operation_metrics")
public class AiOperationMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(name = "target_ref", length = 100)
    private String targetRef;

    @Column(name = "model", length = 50)
    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AiOperationMetric(String operationType, String targetRef, String model,
                              Integer inputTokens, Integer outputTokens,
                              Integer durationMs, boolean success, String errorMessage) {
        this.operationType = operationType;
        this.targetRef = targetRef;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.durationMs = durationMs;
        this.success = success;
        this.errorMessage = errorMessage;
        this.createdAt = LocalDateTime.now();
    }
}
