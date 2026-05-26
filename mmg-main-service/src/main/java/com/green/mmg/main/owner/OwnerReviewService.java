package com.green.mmg.main.owner;

import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.feign.AdminFeignClient;
import com.green.mmg.main.feign.AuthFeignClient;
import com.green.mmg.main.feign.model.ReportReviewReq;
import com.green.mmg.main.owner.entity.ReviewReply;
import com.green.mmg.main.owner.model.*;
import com.green.mmg.main.review.model.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerReviewService {
    private final OwnerReviewRepository ownerReviewRepository;
    private final AuthFeignClient authFeignClient;
    private final OwnerMapper ownerMapper;
    private final ReviewReplyRepository reviewReplyRepository;
    private final AdminFeignClient adminFeignClient;


    @Transactional(readOnly = true)
    public OwnerReviewListRes customerReviewViews(Long ownerNo, OwnerReviewReq req) {
        Long storeOwnerNo = ownerMapper.findStoreOwnerByStoreId(req.getStoreId());

        if (storeOwnerNo == null) {
            throw new BusinessException("가게를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        if (!Objects.equals(storeOwnerNo, ownerNo)) {
            throw new BusinessException("본인 가게의 리뷰만 조회할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        Double avgRating = ownerReviewRepository.findAvgRatingByStoreId(req.getStoreId());

        Map<Integer, Long> ratingStats = new LinkedHashMap<>();
        for(int i = 5; i>=1; i--) {
            ratingStats.put(i, 0L);
        }

        List<OwnerRatingCountRes> counts = ownerReviewRepository.findRatingCountsByStoreId(req.getStoreId());

        counts.forEach(row -> ratingStats.put(row.getRating(), row.getCount()));

        PageRequest pageRequest = PageRequest.of(req.getPage()- 1, req.getSize());
        List<OwnerReviewRes> reviews = ownerReviewRepository.findOwnerReviewByStoreId(req.getStoreId(), pageRequest);

        List<Long> userNos = reviews.stream()
                .map(OwnerReviewRes::getUserNo)
                .distinct()
                .toList();

        if(!userNos.isEmpty()) {
            Map<Long, String> nameMap = authFeignClient.getUsers(userNos)
                    .getResultData()
                    .stream()
                    .collect(Collectors.toMap(UserBriefDto::getUserNo, UserBriefDto::getName));

            reviews.forEach(review ->
                    review.setUserName(nameMap.getOrDefault(review.getUserNo(), "알 수 없음" )));

            Map<Long, OwnerWriterRatingStatsRes> writerStatsMap = ownerReviewRepository
                    .findWriterRatingStatsByUserNos(userNos)
                    .stream()
                    .collect(Collectors.toMap(
                            OwnerWriterRatingStatsRes::getUserNo,
                            stats -> stats
                    ));

            reviews.forEach(review -> {
                OwnerWriterRatingStatsRes stats = writerStatsMap.get(review.getUserNo());

                if (stats != null) {
                    review.setWriterAvgRating(stats.getAvgRating());
                    review.setWriterReviewCount(stats.getReviewCount());
                } else {
                    review.setWriterAvgRating(0.0);
                    review.setWriterReviewCount(0L);
                }
            });

        }
        return new OwnerReviewListRes(avgRating, ratingStats, reviews);
    }

    @Transactional
    public void reportReview(Long ownerNo, Long reviewId, OwnerReviewReportReq req) {
        if (req.getReason() == null || req.getReason().isBlank()) {
            throw new BusinessException("신고 사유를 입력해 주세요.", HttpStatus.BAD_REQUEST);
        }

        Review review = ownerReviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException("리뷰를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Long reviewStoreOwnerNo = ownerMapper.findStoreOwnerByOrderId(review.getOrderId());

        if (!Objects.equals(reviewStoreOwnerNo, ownerNo)) {
            throw new BusinessException("본인 가게의 리뷰만 신고할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        adminFeignClient.reportReview(
                new ReportReviewReq(
                        reviewId,
                        ownerNo,
                        req.getReason(),
                        null
                )
        );

    }

    @Transactional
    public void registerReviewReply(Long ownerNo, Long reviewId, OwnerReviewReplyReq req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException("답글 내용을 입력해 주세요.", HttpStatus.BAD_REQUEST);
        }

        Review review = ownerReviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException("리뷰를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Long reviewStoreOwnerNo = ownerMapper.findStoreOwnerByOrderId(review.getOrderId());

        if (!Objects.equals(reviewStoreOwnerNo, ownerNo)) {
            throw new BusinessException("본인 가게의 리뷰에만 답글을 작성할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        if (reviewReplyRepository.existsByReviewId(reviewId)) {
            throw new BusinessException("이미 답글이 등록된 리뷰입니다.", HttpStatus.CONFLICT);
        }

        ReviewReply reviewReply = new ReviewReply();
        reviewReply.setReviewId(reviewId);
        reviewReply.setOwnerId(ownerNo);
        reviewReply.setContent(req.getContent());

        reviewReplyRepository.save(reviewReply);
    }

    @Transactional
    public void updateReviewReply(Long ownerNo, Long replyId, OwnerReviewReplyUpdateReq req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException("답글 내용을 입력해 주세요.", HttpStatus.BAD_REQUEST);
        }

        ReviewReply reviewReply = reviewReplyRepository.findByReplyIdAndOwnerId(replyId, ownerNo)
                .orElseThrow(() -> new BusinessException("수정할 답글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        reviewReply.setContent(req.getContent());
    }

    @Transactional
    public void deleteReviewReply(Long ownerNo, Long replyId) {
        ReviewReply reviewReply = reviewReplyRepository.findByReplyIdAndOwnerId(replyId, ownerNo)
                .orElseThrow(() -> new BusinessException("삭제할 답글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        reviewReplyRepository.delete(reviewReply);
    }



}
