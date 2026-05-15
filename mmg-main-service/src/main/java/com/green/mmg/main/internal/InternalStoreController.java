package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.exception.ResourceNotFoundException;
import com.green.mmg.main.internal.dto.InternalStoreListPageRes;
import com.green.mmg.main.store.StoreService;
import com.green.mmg.main.store.model.StoreOneGetRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/store")
public class InternalStoreController {
    private final StoreService storeService;

    @GetMapping("/list")
    public ResultResponse<InternalStoreListPageRes> getStoreList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String date) {
        return new ResultResponse<>("가게 목록 조회 완료",
                storeService.getInternalStoreList(page, size, date));
    }



    @GetMapping("/{storeId}")
    public ResultResponse<StoreOneGetRes> getStoreDetail(@PathVariable long storeId) {
        StoreOneGetRes result = storeService.storeOneGet(storeId);

        if (result == null) {
            throw new ResourceNotFoundException("store not found: " + storeId);
        }

        return new ResultResponse<>("가게 상세 조회 완료", result);
    }



}
