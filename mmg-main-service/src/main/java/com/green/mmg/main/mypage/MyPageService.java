package com.green.mmg.main.mypage;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.coupon.CouponListRepository;
import com.green.mmg.main.feign.AuthFeignClient;
import com.green.mmg.main.feign.model.InternalUserDetailRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final AuthFeignClient authFeignClient;
    private final CouponListRepository couponListRepository;

    @Transactional(readOnly = true)
    public MyPageSummaryRes getSummary(Long userNo) {
        ResultResponse<InternalUserDetailRes> response = authFeignClient.getUserDetail(userNo);
        InternalUserDetailRes user = response == null ? null : response.getResultData();
        if (user == null) {
            throw new BusinessException("회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        int green = user.getGreen() == null ? 0 : user.getGreen();
        long usableCouponCount = couponListRepository.countUsableCouponsByUserNo(userNo, LocalDateTime.now());

        return new MyPageSummaryRes(
                user.getUserNo(),
                user.getUserId(),
                user.getName(),
                user.getTel(),
                green,
                resolveGreenGrade(green),
                usableCouponCount
        );
    }

    private GreenGradeRes resolveGreenGrade(int green) {
        if (green < 10) {
            return grade("씨앗", green, 0, 9, "새싹", 10 - green);
        }
        if (green < 15) {
            return grade("새싹", green, 10, 14, "나무", 15 - green);
        }
        if (green < 40) {
            return grade("나무", green, 15, 39, "숲", 40 - green);
        }
        if (green < 62) {
            return grade("숲", green, 40, 61, "지구", 62 - green);
        }
        return new GreenGradeRes("지구", green, 62, null, null, 0, 100);
    }

    private GreenGradeRes grade(String gradeName,
                                int currentPoint,
                                int minPoint,
                                int maxPoint,
                                String nextGradeName,
                                int pointToNextGrade) {
        int range = Math.max(maxPoint - minPoint + 1, 1);
        int progressed = Math.min(Math.max(currentPoint - minPoint + 1, 0), range);
        int progressPercent = progressed * 100 / range;
        return new GreenGradeRes(
                gradeName,
                currentPoint,
                minPoint,
                maxPoint,
                nextGradeName,
                pointToNextGrade,
                progressPercent
        );
    }
}
