package com.green.mmg.main.pet;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.pet.dto.PetRes;
import com.green.mmg.main.pet.dto.PetRewardRes;
import com.green.mmg.main.pet.dto.PetUpdateReq;
import com.green.mmg.main.pet.entity.GreenPointLog;
import com.green.mmg.main.pet.entity.Pet;
import com.green.mmg.main.pet.entity.PetSpecies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final GreenPointLogRepository greenPointLogRepository;

    // P-6 보상 정책 (학원 발표용 단순 박제)
    private static final int INTIMACY_PER_ORDER = 5;
    private static final int EXP_BASE = 30;
    private static final int EXP_PER_10000_KRW = 10;
    private static final int LEVEL_UP_POINT_REWARD = 100;

    /**
     * 회원가입 시 자동 지급 + lazy fallback 진입점.
     * 이미 펫이 있으면 그대로 반환 (idempotent).
     * Q3 (다-1): auth signup commit 후 별도 Feign 호출 — 실패 시 다음 펫 접근 시 자동 생성.
     */
    @Transactional
    public Pet createInitialPetIfAbsent(Long userNo, PetSpecies species, String name) {
        return petRepository.findByUserNo(userNo).orElseGet(() -> {
            PetSpecies resolvedSpecies = species == null ? PetSpecies.DOG : species;
            String resolvedName = (name == null || name.isBlank()) ? "펫" + userNo : name;
            return petRepository.save(new Pet(userNo, resolvedSpecies, resolvedName));
        });
    }

    @Transactional(readOnly = true)
    public PetRes getMyPet(Long userNo) {
        Pet pet = petRepository.findByUserNo(userNo)
                .orElseThrow(() -> new BusinessException("펫이 없습니다.", HttpStatus.NOT_FOUND));
        return new PetRes(pet);
    }

    /** 펫이 없으면 lazy 생성 후 반환. P-3 챗봇 진입 시 호출 — 회원가입 자동 지급 실패 보상. */
    @Transactional
    public Pet getOrCreatePet(Long userNo) {
        return petRepository.findByUserNo(userNo)
                .orElseGet(() -> petRepository.save(new Pet(userNo, PetSpecies.DOG, "펫" + userNo)));
    }

    /**
     * P-6 주문 완료 시 간식 자동 지급 — OrderService.completeDelivery에서 호출.
     * 1주문 1회 idempotent (green_point_log.order_id UNIQUE).
     * 펫 부재 시 lazy 생성. 친밀도 +5, EXP +기본30+(amount/10000)*10. 레벨업 발생 시 포인트 보상 INSERT.
     */
    @Transactional
    public PetRewardRes grantOrderReward(Long userNo, Long orderId, int orderAmount) {
        if (greenPointLogRepository.existsByOrderId(orderId)) {
            log.info("주문 보상 이미 적립됨 — orderId={}", orderId);
            Pet pet = petRepository.findByUserNo(userNo)
                    .orElseThrow(() -> new BusinessException("펫이 없습니다.", HttpStatus.NOT_FOUND));
            return new PetRewardRes(pet.getPetNo(), pet.getLevel(), pet.getExp(), pet.getIntimacy(), false, 0);
        }

        Pet pet = petRepository.findByUserNo(userNo)
                .orElseGet(() -> petRepository.save(new Pet(userNo, PetSpecies.DOG, "펫" + userNo)));

        int expGain = EXP_BASE + Math.max(0, orderAmount / 10000) * EXP_PER_10000_KRW;
        boolean leveledUp = pet.gainExp(expGain, INTIMACY_PER_ORDER);

        int pointReward = 0;
        if (leveledUp) {
            pointReward = pet.getLevel() * LEVEL_UP_POINT_REWARD;
            try {
                greenPointLogRepository.save(new GreenPointLog(orderId, userNo, pointReward,
                        "펫 레벨업 보상 (Lv." + pet.getLevel() + ")"));
            } catch (DataIntegrityViolationException race) {
                // 동시 race — 다른 트랜잭션이 INSERT 성공. 보상 자체는 그대로 진행.
                log.warn("green_point_log UNIQUE race 회피 — orderId={}", orderId);
            }
        }
        return new PetRewardRes(pet.getPetNo(), pet.getLevel(), pet.getExp(), pet.getIntimacy(), leveledUp, pointReward);
    }

    @Transactional
    public PetRes updatePet(Long userNo, PetUpdateReq req) {
        Pet pet = petRepository.findByUserNo(userNo)
                .orElseThrow(() -> new BusinessException("펫이 없습니다.", HttpStatus.NOT_FOUND));
        pet.rename(req.getName());
        pet.changeSpecies(req.getSpecies());
        return new PetRes(pet);
    }
}
