package com.green.mmg.main.review.model;

import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * review 테이블 엔티티 (my_mmg_main.review).
 *
 * <p>Phase 3-C-2: BaseEntity 첫 검증 도메인.
 * write_at/amended_at은 BaseEntity의 createdAt/updatedAt에 매핑 (@AttributeOverride).</p>
 *
 * <p>BaseEntity의 @CreatedDate/@LastModifiedDate가 자동 채움 — DB DEFAULT current_timestamp()
 * 대신 JPA Auditing이 LocalDateTime.now() 셋팅. 응답 노출은 ReviewRes에 없음 (영향 0).</p>
 */
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
}
