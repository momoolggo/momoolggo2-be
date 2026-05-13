package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.feign.AuthFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/rider")
public class InternalRiderController {
    private final AuthFeignClient authFeignClient;

    @Transactional(readOnly = true)
    @GetMapping("/count")
    public ResultResponse<Long> getRiderCount() {
        Long total = authFeignClient.getRiderCount().getResultData();
        return new ResultResponse<>("라이더 수 조회 완료", total);
    }

}
