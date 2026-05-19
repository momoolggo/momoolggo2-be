package com.green.mmg.main.review;

import com.green.mmg.main.internal.dto.InternalReviewListRes;
import com.green.mmg.main.review.model.GetReviewReq;
import com.green.mmg.main.review.model.ReviewReq;
import com.green.mmg.main.review.model.ReviewRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewMapper {

    // postReview는 ReviewRepository.saveAndFlush로 대체 (Phase 3-C-2 BaseEntity 첫 검증)

    long checkReviewWriter(ReviewReq req);

    List<ReviewRes> getReviews(GetReviewReq req);
    int countReviews(long userNo);
    int deleteReview(@Param("userNo") long userNo, @Param("reviewId") long reviewId);

    // 리뷰 단건 조회
    Map<String, Object> getReviewById(long reviewId);

    // 리뷰 수정
    int updateReview(@Param("reviewId") long reviewId,
                     @Param("userNo") long userNo,
                     @Param("rating") int rating,
                     @Param("contents") String contents);

    // 리뷰로 가게 ID 조회 (별점 갱신용)
    long findStoreIdByReviewId(long reviewId);

    // 주문으로 가게 ID 조회 (별점 갱신용)
    long findStoreIdByOrderId(long orderId);

    // 가게 별점/리뷰수 갱신
    void updateStoreRating(long storeId);

    //전체 리뷰 목록
    List<InternalReviewListRes> findInternalReviewList(
            @Param("startIdx") int startIdx,
            @Param("size") int size
    );

    // 리뷰 단건 조회 (internal)
    InternalReviewListRes findInternalReviewById(@Param("reviewId") Long reviewId);


}
