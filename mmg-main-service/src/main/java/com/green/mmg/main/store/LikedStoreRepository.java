package com.green.mmg.main.store;

import com.green.mmg.main.store.model.LikedStore;
import com.green.mmg.main.store.model.LikedStoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Phase 3-B-2: LikedStore CRUD JPA. favoriteList(JOIN+LIMIT)는 StoreMapper에 잔존
 * (하이브리드 영구 공존 — 같은 도메인 내 Repository + Mapper 동시 사용).
 */
public interface LikedStoreRepository extends JpaRepository<LikedStore, LikedStoreId> {

    boolean existsByUserNoAndStoreId(long userNo, long storeId);

    long countByUserNo(long userNo);

    @Modifying
    @Query("DELETE FROM LikedStore l WHERE l.userNo = :userNo AND l.storeId = :storeId")
    int deleteByUserNoAndStoreId(@Param("userNo") long userNo, @Param("storeId") long storeId);
}
