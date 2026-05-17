package com.green.mmg.main.owner;

import com.green.mmg.main.owner.model.OwnerRatingCountRes;
import com.green.mmg.main.owner.model.OwnerReviewRes;
import com.green.mmg.main.owner.model.OwnerWriterRatingStatsRes;
import com.green.mmg.main.review.model.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OwnerReviewRepository extends JpaRepository<Review, Long> {

    @Query("""
        SELECT COALESCE(AVG(r.rating), 0)
        FROM Review r
        JOIN Orders o ON o.orderId = r.orderId
        WHERE o.storeId = :storeId
    """)
    Double findAvgRatingByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT new com.green.mmg.main.owner.model.OwnerRatingCountRes(
            r.rating,
            COUNT(r)
        )
        FROM Review r
        JOIN Orders o ON o.orderId = r.orderId
        WHERE o.storeId = :storeId
        GROUP BY r.rating
    """)
    List<OwnerRatingCountRes> findRatingCountsByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT new com.green.mmg.main.owner.model.OwnerWriterRatingStatsRes(
            o.userNo,
            AVG(r.rating),
            COUNT(r)
        )
        FROM Review r
        JOIN Orders o ON o.orderId = r.orderId
        WHERE o.userNo IN :userNos
        GROUP BY o.userNo
    """)
    List<OwnerWriterRatingStatsRes> findWriterRatingStatsByUserNos(@Param("userNos") List<Long> userNos);

    @Query("""
        SELECT new com.green.mmg.main.owner.model.OwnerReviewRes(
            r.reviewId,
            o.userNo,
            '',
            MIN(od.menuName),
            r.rating,
            r.contents,
            r.photo,
            r.createdAt,
            r.updatedAt,
            null,
            null,
            rr.replyId,
            rr.content,
            rr.writtenAt
        )
        FROM Review r
        JOIN Orders o ON o.orderId = r.orderId
        JOIN OrderDetail od ON od.orderId = r.orderId
        LEFT JOIN ReviewReply rr ON rr.reviewId = r.reviewId
        WHERE o.storeId = :storeId
        GROUP BY
            r.reviewId,
            o.userNo,
            r.rating,
            r.contents,
            r.photo,
            r.createdAt,
            r.updatedAt,
            rr.replyId,
            rr.content,
            rr.writtenAt
        ORDER BY r.createdAt DESC
    """)
    List<OwnerReviewRes> findOwnerReviewByStoreId(@Param("storeId") Long storeId, Pageable pageable);
}
