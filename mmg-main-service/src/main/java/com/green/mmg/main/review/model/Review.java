package com.green.mmg.main.review.model;

import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Getter
@Setter
@NoArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "written_at", updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "amended_at"))
})
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "contents", length = 1000)
    private String contents;

    @Column(name = "photo", length = 1000)
    private String photo;

    // 블라인드 필드
    @Column(name = "blinded", nullable = false)
    private boolean blinded = false;

    @Column(name = "blinded_at")
    private LocalDateTime blindedAt;

    @Column(name = "blind_source", length = 20)
    private String blindSource;

    @Column(name = "blind_reason", length = 200)
    private String blindReason;

    @Column(name = "blind_report_id")
    private Long blindReportId;

    public void applyBlind(String source, String reason, Long reportId) {
        this.blinded = true;
        this.blindedAt = LocalDateTime.now();
        this.blindSource = source;
        this.blindReason = reason;
        this.blindReportId = reportId;
    }

    public void releaseBlind() {
        this.blinded = false;
        this.blindedAt = null;
        this.blindSource = null;
        this.blindReason = null;
        this.blindReportId = null;
    }
}

