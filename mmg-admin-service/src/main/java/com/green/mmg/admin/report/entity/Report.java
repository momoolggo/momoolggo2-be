package com.green.mmg.admin.report.entity;

import com.green.mmg.admin.common.enums.ReportStatus;
import com.green.mmg.admin.common.enums.ReportTargetType;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_no", nullable = false)
    private Long targetNo;

    @Column(name = "reason", nullable = false, length = 50)
    private String reason;

    @Column(name = "content", length = 500)
    private String content;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}