package com.green.mmg.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA 공통 Auditing entity. created_at / updated_at 컬럼이 있는 테이블만 상속.
 *
 * <p>활성화 조건: 사용하는 서비스의 메인 클래스에 {@code @EnableJpaAuditing} 필요.</p>
 *
 * <p>Phase 3-A 정책: user 테이블은 created_at/updated_at 미존재 → 상속 X.
 * Phase 3-B의 likedstore, store 등 컬럼 있는 도메인부터 적용.</p>
 *
 * <p>MyBatis와 하이브리드 사용 시 주의: BaseEntity의 Auditing은 JPA에서만 자동 동작.
 * 같은 테이블을 MyBatis로 INSERT/UPDATE 할 때는 SQL에 NOW() 명시 필요.</p>
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
