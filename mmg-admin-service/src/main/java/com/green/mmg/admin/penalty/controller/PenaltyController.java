package com.green.mmg.admin.penalty.controller;

import com.green.mmg.admin.penalty.dto.PenaltyReq;
import com.green.mmg.admin.penalty.service.PenaltyService;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class PenaltyController {

    private final PenaltyService penaltyService;

    // 패널티 부여
    @PutMapping("/user/{userNo}/penalty")
    public ResultResponse<?> givePenalty(@PathVariable Long userNo,
                                         @RequestBody PenaltyReq req) {
        penaltyService.givePenalty(req);
        return new ResultResponse<>("패널티 부여 완료", null);
    }

    // 패널티 취소
    @PatchMapping("/penalties/{penaltyId}/cancel")
    public ResultResponse<?> cancelPenalty(@PathVariable Long penaltyId) {
        penaltyService.cancelPenalty(penaltyId);
        return new ResultResponse<>("패널티 취소 완료", null);
    }
}