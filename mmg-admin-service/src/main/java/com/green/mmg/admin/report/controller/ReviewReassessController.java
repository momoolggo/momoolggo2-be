package com.green.mmg.admin.report.controller;

import com.green.mmg.admin.report.ai.ReviewReassessService;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/review")
@RequiredArgsConstructor
public class ReviewReassessController {

    private final ReviewReassessService reassessService;

    @PostMapping("/{reviewId}/reassess")
    public ResultResponse<?> reassess(
            @PathVariable Long reviewId,
            @RequestBody ReassessReq req) {
        reassessService.reassess(reviewId, req.updatedContent());
        return new ResultResponse<>("재판정 완료", null);
    }

    public record ReassessReq(String updatedContent) {}
}
