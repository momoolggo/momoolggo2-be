package com.green.mmg.main.coupon;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.coupon.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CouponRewardCodeService {

    private static final String EVENT_ISSUE_TYPE = "EVENT";
    private static final String FOOD_CUP_EVENT_CODE = "FOOD_CUP";
    private static final int CODE_EXPIRE_HOURS = 24;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Map<Integer, String> STAGE_REWARD_KEYS = Map.of(
            1, "FOOD_CUP_STAGE_1",
            2, "FOOD_CUP_STAGE_2",
            3, "FOOD_CUP_STAGE_3",
            4, "FOOD_CUP_STAGE_4",
            5, "FOOD_CUP_STAGE_5"
    );

    private final CouponRepository couponRepository;
    private final CouponListRepository couponListRepository;
    private final CouponRewardCodeRepository rewardCodeRepository;

    @Transactional
    public CouponRewardCodeCreateRes createFoodCupRewardCode(Long userNo, Integer stage) {
        int rewardStage = validateStage(stage);
        LocalDate today = LocalDate.now();

        CouponRewardCode existing = rewardCodeRepository
                .findByUserNoAndEventCodeAndIssueDate(userNo, FOOD_CUP_EVENT_CODE, today)
                .orElse(null);

        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getUsed())) {
                throw new BusinessException("오늘은 이미 이벤트 쿠폰을 등록했습니다.", HttpStatus.BAD_REQUEST);
            }

            Coupon coupon = couponRepository.findById(existing.getCouponId())
                    .orElseThrow(() -> new BusinessException("이벤트 보상 쿠폰을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

            return new CouponRewardCodeCreateRes(
                    existing.getCode(),
                    existing.getRewardStage(),
                    coupon.getCouponId(),
                    coupon.getName(),
                    existing.getExpiresAt()
            );
        }

        String rewardKey = STAGE_REWARD_KEYS.get(rewardStage);

        Coupon coupon = couponRepository
                .findFirstByDescriptionAndIssueTypeAndIsActiveTrue(rewardKey, EVENT_ISSUE_TYPE)
                .orElseThrow(() -> new BusinessException("이벤트 보상 쿠폰을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        CouponRewardCode rewardCode = rewardCodeRepository.save(
                CouponRewardCode.create(
                        userNo,
                        FOOD_CUP_EVENT_CODE,
                        rewardStage,
                        coupon.getCouponId(),
                        generateUniqueCode(),
                        today,
                        LocalDateTime.now().plusHours(CODE_EXPIRE_HOURS)
                )
        );

        return new CouponRewardCodeCreateRes(
                rewardCode.getCode(),
                rewardStage,
                coupon.getCouponId(),
                coupon.getName(),
                rewardCode.getExpiresAt()
        );
    }

    @Transactional
    public CouponCodeIssueRes issueCouponByCode(Long userNo, String code) {
        String normalizedCode = normalizeCode(code);
        LocalDateTime now = LocalDateTime.now();

        CouponRewardCode rewardCode = rewardCodeRepository.findByUserNoAndCode(userNo, normalizedCode)
                .orElseThrow(() -> new BusinessException("유효하지 않은 쿠폰 코드입니다.", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(rewardCode.getUsed())) {
            throw new BusinessException("이미 사용한 쿠폰 코드입니다.", HttpStatus.BAD_REQUEST);
        }

        if (rewardCode.getExpiresAt().isBefore(now)) {
            throw new BusinessException("만료된 쿠폰 코드입니다.", HttpStatus.BAD_REQUEST);
        }

        Coupon coupon = couponRepository.findById(rewardCode.getCouponId())
                .orElseThrow(() -> new BusinessException("쿠폰 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        CouponList couponList = couponListRepository.saveAndFlush(
                CouponList.issue(userNo, coupon, now)
        );

        rewardCode.markUsed(couponList.getCouponListId());

        return new CouponCodeIssueRes(
                couponList.getCouponListId(),
                coupon.getCouponId(),
                coupon.getName(),
                couponList.getExpiresAt()
        );
    }

    private int validateStage(Integer stage) {
        if (stage == null || stage < 1 || stage > 5) {
            throw new BusinessException("보상 단계가 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        return stage;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("쿠폰 코드를 입력해 주세요.", HttpStatus.BAD_REQUEST);
        }
        return code.trim().toUpperCase();
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = "MMG-" + randomPart(6);
        } while (rewardCodeRepository.existsByCode(code));
        return code;
    }

    private String randomPart(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}