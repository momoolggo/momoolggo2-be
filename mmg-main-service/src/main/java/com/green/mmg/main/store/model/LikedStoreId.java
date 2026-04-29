package com.green.mmg.main.store.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * likedstore 복합 PK (user_no + store_id).
 *
 * <p>{@link jakarta.persistence.IdClass} 요구사항:
 * Serializable, no-arg, equals/hashCode 필수.
 * 필드 이름/타입은 LikedStore 엔티티의 @Id 필드와 동일.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LikedStoreId implements Serializable {
    private long userNo;
    private long storeId;
}
