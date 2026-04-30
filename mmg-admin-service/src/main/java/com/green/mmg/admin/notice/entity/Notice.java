package com.green.mmg.admin.notice.entity;

import com.green.mmg.admin.common.enums.NoticeSendType;
import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "notice")
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long noticeId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 300)
    private String content;

    @Column(name = "target", length = 20)
    private String target;

    @Column(name = "region_filter", length = 20)
    private String regionFilter;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_type", length = 20)
    private NoticeSendType sendType;

    @Column(name = "send_at")
    private LocalDateTime sendAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}