package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalSettlementController {

    @GetMapping("settlement")
    public ResultResponse<Void> getSettlement(@RequestParam  String startDate,
                                              @RequestParam String endDate) {
        //TODO: 정산 조회 로직구현예정
        return new ResultResponse<>("정산 조회 완료", null);
    }

}
