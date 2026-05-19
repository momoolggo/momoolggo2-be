package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.main.feign.AuthFeignClient;
import com.green.mmg.main.internal.dto.InternalReviewListRes;
import com.green.mmg.main.review.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/internal/review")
public class InternalReviewController {
    private final ReviewMapper reviewMapper;
    private final AuthFeignClient authFeignClient;

    @PutMapping("/{reviewId}/action")
    public ResultResponse<Void> reviewAction(@PathVariable Long reviewId,
                                         @RequestBody Map<String, String> body) {
        //TODO: 리뷰 로직 구현 예정
        return new ResultResponse<>("리뷰 처리 완료", null);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{reviewId}")
    public ResultResponse<InternalReviewListRes> findInternalReviewById(@PathVariable Long reviewId) {
        InternalReviewListRes review = reviewMapper.findInternalReviewById(reviewId);
        if (review == null) return new ResultResponse<>("리뷰 없음", null);
        try {
            List<UserBriefDto> users = authFeignClient.getUsers(List.of(review.getUserNo())).getResultData();
            if (users != null && !users.isEmpty()) {
                review.setWriter(users.get(0).getName());
            }
        } catch (Exception ignored) {}
        return new ResultResponse<>("리뷰 조회 완료", review);
    }

    @Transactional(readOnly = true)
    @GetMapping("/list")
    public ResultResponse<List<InternalReviewListRes>> findInternalReviewList(@RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "10") int size){
        int startIdx = page * size;

        List<InternalReviewListRes> reviews = reviewMapper.findInternalReviewList(startIdx, size);

        List<Long> userNos = reviews.stream()
                .map(InternalReviewListRes::getUserNo)
                .distinct()
                .toList();

        if(!userNos.isEmpty()) {
            List<UserBriefDto> users = authFeignClient.getUsers(userNos).getResultData();

            Map<Long, String> writerMap = (users == null ? List.<UserBriefDto>of() :users)
                    .stream()
                    .collect(Collectors.toMap(UserBriefDto::getUserNo, UserBriefDto::getName));

            reviews.forEach(review ->
                    review.setWriter(writerMap.getOrDefault(review.getUserNo(), "")));
        }
        return new ResultResponse<>("리뷰 목록 조회 완료", reviews);

    }
}
