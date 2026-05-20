package com.green.mmg.admin.store.controller;

import com.green.mmg.admin.dto.feign.InternalStoreListPageRes;
import com.green.mmg.admin.dto.feign.InternalStoreListRes;
import com.green.mmg.admin.feign.MainFeignClient;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/store")
@RequiredArgsConstructor
public class AdminStoreController {

    private final MainFeignClient mainFeignClient;

    /** 가게 목록 조회 */
    @GetMapping
    public ResultResponse<InternalStoreListPageRes> getStoreList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String storeName,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category
    ) {
        return mainFeignClient
                .getStoreList(page, size, storeName, businessNo, userId, date, name, category);
    }
    @GetMapping("/{storeId}")
    public ResultResponse<?> getStoreDetail(@PathVariable Long storeId) {
        return mainFeignClient.getStoreDetail(storeId);
    }
}
