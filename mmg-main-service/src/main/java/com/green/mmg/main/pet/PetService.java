package com.green.mmg.main.pet;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.pet.dto.PetRes;
import com.green.mmg.main.pet.dto.PetUpdateReq;
import com.green.mmg.main.pet.entity.Pet;
import com.green.mmg.main.pet.entity.PetSpecies;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;

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

    @Transactional
    public PetRes updatePet(Long userNo, PetUpdateReq req) {
        Pet pet = petRepository.findByUserNo(userNo)
                .orElseThrow(() -> new BusinessException("펫이 없습니다.", HttpStatus.NOT_FOUND));
        pet.rename(req.getName());
        pet.changeSpecies(req.getSpecies());
        return new PetRes(pet);
    }
}
