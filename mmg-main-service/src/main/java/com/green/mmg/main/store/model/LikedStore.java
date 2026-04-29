package com.green.mmg.main.store.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * likedstore 테이블 엔티티 (my_mmg_main.likedstore).
 *
 * <p>복합 PK: (user_no, store_id) — {@link LikedStoreId} 사용.</p>
 *
 * <p>BaseEntity 미상속 사유: created_at만 있고 updated_at 컬럼 부재 (Phase 3-B 정찰 결과).
 * created_at은 DB DEFAULT current_timestamp() 사용 — JPA insertable=false.</p>
 */
@Entity
@Table(name = "likedstore")
@IdClass(LikedStoreId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LikedStore {

    @Id
    @Column(name = "user_no")
    private long userNo;

    @Id
    @Column(name = "store_id")
    private long storeId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
