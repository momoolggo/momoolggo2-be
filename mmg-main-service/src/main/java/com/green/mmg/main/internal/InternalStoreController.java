package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.exception.ResourceNotFoundException;
import com.green.mmg.main.store.StoreService;
import com.green.mmg.main.store.model.StoreOneGetRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/store")
public class InternalStoreController {
    private final StoreService storeService;

    @GetMapping("/{storeId}")
    public ResultResponse<StoreOneGetRes> getStoreDetail(@PathVariable long storeId) {
        StoreOneGetRes result = storeService.storeOneGet(storeId);

        if (result == null) {
            throw new ResourceNotFoundException("store not found: " + storeId);
        }

        return new ResultResponse<>("가게 상세 조회 완료", result);
    }

    @PutMapping("/{storeId}/approve")
    public ResultResponse<Void> approveStore(@PathVariable Long storeId) {
        //TODO: 가게 승인 로직 구현 예정
        return new ResultResponse<>("가게 승인 완료", null);
    }



}
