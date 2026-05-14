package com.green.mmg.admin.review.controller;

import com.green.mmg.admin.dto.feign.InternalReviewListRes;
import com.green.mmg.admin.feign.MainFeignClient;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final MainFeignClient mainFeignClient;

    @GetMapping
    public ResultResponse<List<InternalReviewListRes>> getReviewList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return mainFeignClient.getReviewList(page, size);
    }
}