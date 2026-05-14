package com.green.mmg.main.review;

import com.green.mmg.main.review.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * Phase 3-C-2: Review postReview만 JPA 전환 (단순 INSERT).
 * 9개 복잡 쿼리(다중 테이블 UPDATE/DELETE, 집계, JOIN)는 ReviewMapper에 영구 잔존.
 *
 * <p>BaseEntity 첫 검증 도메인 — write_at/amended_at @AttributeOverride 매핑.</p>
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {
    //오늘 리뷰 수
    @Query("""
            SELECT COUNT(r)
            FROM Review r
            WHERE r.createdAt >= :start
            AND r.createdAt < :end
            """)

    long countTodayReviews(@Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

}
