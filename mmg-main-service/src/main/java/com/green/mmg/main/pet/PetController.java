package com.green.mmg.main.pet;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.pet.dto.PetRes;
import com.green.mmg.main.pet.dto.PetSnackRes;
import com.green.mmg.main.pet.dto.PetUpdateReq;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pet")
public class PetController {

    private final PetService petService;

    @GetMapping("/me")
    public ResultResponse<PetRes> getMyPet(@AuthenticationPrincipal UserPrincipal principal) {
        return new ResultResponse<>("펫 조회 완료", petService.getMyPet(principal.getSignedUserNo()));
    }

    @PutMapping("/me")
    public ResultResponse<PetRes> updatePet(@AuthenticationPrincipal UserPrincipal principal,
                                            @RequestBody PetUpdateReq req) {
        return new ResultResponse<>("펫 정보 수정 완료", petService.updatePet(principal.getSignedUserNo(), req));
    }

    /** 2026-05-25 9건 트랙 #8 부채 — 간식주기 (펫 직접 상호작용) */
    @PostMapping("/me/snack")
    public ResultResponse<PetSnackRes> giveSnack(@AuthenticationPrincipal UserPrincipal principal) {
        return new ResultResponse<>("간식 지급 완료", petService.giveSnack(principal.getSignedUserNo()));
    }
}
