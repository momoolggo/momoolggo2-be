package com.green.mmg.main.owner;

import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.main.feign.AuthFeignClient;
import com.green.mmg.main.owner.model.OwnerRatingCountRes;
import com.green.mmg.main.owner.model.OwnerReviewListRes;
import com.green.mmg.main.owner.model.OwnerReviewReq;
import com.green.mmg.main.owner.model.OwnerReviewRes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerReviewService {
    private final OwnerReviewRepository ownerReviewRepository;
    private final AuthFeignClient authFeignClient;

    @Transactional(readOnly = true)
    public OwnerReviewListRes customerReviewViews(OwnerReviewReq req) {
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

        }
        return new OwnerReviewListRes(avgRating, ratingStats, reviews);
    }

}
