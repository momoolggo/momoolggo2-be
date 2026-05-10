package com.green.mmg.rider.notice.model;

import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * notice 테이블 엔티티 (my_mmg_rider.notice).
 *
 * <p>외부 참조: {@code sender_admin_no} → my_mmg_admin.admin (논리 FK, 물리 FK 제약 X — CLAUDE.md §3 MSA 경계).</p>
 *
 * <p>작성 흐름 (ADR-009): admin-service Feign 호출 → rider-service POST /internal/notice → INSERT.
 * 라이더 측 GET만 (ROLE_RIDER), 작성/수정/삭제는 admin-service 진입점.</p>
 *
 * <p>BaseEntity 상속: created_at / updated_at 컬럼 자동 매핑 (R1-A Rider / R2-a Delivery 패턴 일관).
 * UPDATE 다수 도메인 (admin PUT으로 수정 가능 — ADR-009 line 97 PUT /internal/notice/{noticeId}).</p>
 *
 * <p>setter 미공개 — update 등 명시 메서드는 R9 NoticeService 진입 시 추가.
 * R2 시점은 entity 형태 + 신규 INSERT 시점 생성자만 도입.</p>
 *
 * <p>관련 ADR: ADR-002 line 184-194 (정정 8) + ADR-009 (Phase 5-R9 본격 구현).
 * 인덱스 1건: published_at (Q-R2a2 (나) 자동 적용, ADR-009 line 201 가시성 필터).</p>
 */
@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_no")
    private Long noticeNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private NoticeCategory category;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20, nullable = false)
    private NoticeTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_type", length = 20, nullable = false)
    private NoticeSendType sendType;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "sender_admin_no", nullable = false)
    private Long senderAdminNo;

    /**
     * 신규 INSERT 시점 생성자 — POST /internal/rider/notice 진입점.
     * sendType=NOW: publishedAt=now, reservedAt=null. sendType=RESERVED: publishedAt=reservedAt, reservedAt=reservedAt (호출자 검증 후 전달).
     */
    public Notice(NoticeCategory category, String title, String content,
                  NoticeTargetType targetType, NoticeSendType sendType,
                  LocalDateTime reservedAt, LocalDateTime publishedAt, Long senderAdminNo) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.targetType = targetType;
        this.sendType = sendType;
        this.reservedAt = reservedAt;
        this.publishedAt = publishedAt;
        this.senderAdminNo = senderAdminNo;
    }

    // 비즈니스 메서드 (update / delete) — R9 NoticeService 진입 시 추가.
}
