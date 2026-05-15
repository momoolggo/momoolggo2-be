package com.green.mmg.main.feign;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.feign.model.ReportReviewReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "mmg-admin-service",
        url = "${feign.admin-service.url:http://localhost:8083}"
)
public interface AdminFeignClient {

    @PostMapping("/internal/report/review")
    ResultResponse<Void> reportReview(@RequestBody ReportReviewReq req);
}
