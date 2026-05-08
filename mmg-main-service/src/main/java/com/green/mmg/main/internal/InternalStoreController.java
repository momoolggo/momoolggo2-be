package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/store")
public class InternalStoreController {

    @PutMapping("/{storeId}/approve")
    public ResultResponse<Void> approveStore(@PathVariable Long storeId) {
        //TODO: 가게 승인 로직 구현 예정
        return new ResultResponse<>("가게 승인 완료", null);
    }



}
