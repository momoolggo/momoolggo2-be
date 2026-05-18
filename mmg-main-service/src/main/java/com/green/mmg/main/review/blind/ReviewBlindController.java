package com.green.mmg.main.review.blind;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/reviews")
@RequiredArgsConstructor
public class ReviewBlindController {

    private final ReviewBlindService blindService;

    @PostMapping("/{reviewId}/blind")
    public void blind(@PathVariable Long reviewId, @RequestBody BlindRequest req) {
        blindService.blind(reviewId, req.source(), req.reason(), req.reportId());
    }

    @PostMapping("/{reviewId}/unblind")
    public void unblind(@PathVariable Long reviewId) {
        blindService.unblind(reviewId);
    }

    public record BlindRequest(String source, String reason, Long reportId) {}
}
