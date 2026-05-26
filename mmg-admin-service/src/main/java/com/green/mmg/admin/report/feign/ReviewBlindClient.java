package com.green.mmg.admin.report.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mmg-main-service-blind", url = "${feign.main-service.url}")
public interface ReviewBlindClient {

    @PostMapping("/internal/reviews/{reviewId}/blind")
    void blind(@PathVariable("reviewId") Long reviewId, @RequestBody BlindRequest req);

    @PostMapping("/internal/reviews/{reviewId}/unblind")
    void unblind(@PathVariable("reviewId") Long reviewId, @RequestBody UnblindRequest req);

    record BlindRequest(String source, String reason, Long reportId) {}
    record UnblindRequest(Long adminId, String reason) {}
}
