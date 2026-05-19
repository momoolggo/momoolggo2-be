package com.green.mmg.main.feign;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.feign.model.ReportReviewReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "mmg-admin-service",
        url = "${feign.admin-service.url:http://localhost:8083}"
)
public interface AdminFeignClient {

    @PostMapping("/internal/report/review")
    ResultResponse<Void> reportReview(@RequestBody ReportReviewReq req);

    @GetMapping("/api/admin/settlement/internal/store/{storeId}")
    ResultResponse<List<Object>> getSettlementsByStore(@PathVariable("storeId") Long storeId);

    @PostMapping("/api/admin/cs/internal/inquiry")
    ResultResponse<Void> createInquiry(@RequestBody Map<String, Object> req);

    @PostMapping("/internal/review/{reviewId}/reassess")
    ResultResponse<Void> reassessReview(@PathVariable("reviewId") Long reviewId,
                                        @RequestBody Map<String, String> req);

    @PostMapping("/internal/review/{reviewId}/auto-detect")
    ResultResponse<Void> autoDetect(@PathVariable("reviewId") Long reviewId,
                                    @RequestBody Map<String, String> req);
}
