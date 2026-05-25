package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.internal.dto.PetInitReq;
import com.green.mmg.main.internal.dto.PetInitRes;
import com.green.mmg.main.pet.PetService;
import com.green.mmg.main.pet.entity.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/pet")
public class InternalPetController {

    private final PetService petService;

    /**
     * 회원가입 자동 지급 — auth signup commit 후 호출.
     * idempotent: 이미 펫이 있으면 그대로 반환 (lazy fallback과 동일 동작).
     */
    @PostMapping("/init")
    public ResultResponse<PetInitRes> initPet(@RequestBody PetInitReq req) {
        if (req == null || req.getUserNo() == null) {
            throw new BusinessException("userNo가 필요합니다.", HttpStatus.BAD_REQUEST);
        }
        Pet pet = petService.createInitialPetIfAbsent(req.getUserNo(), req.getSpecies(), req.getName());
        return new ResultResponse<>("펫 자동 지급 완료", new PetInitRes(pet));
    }
}
