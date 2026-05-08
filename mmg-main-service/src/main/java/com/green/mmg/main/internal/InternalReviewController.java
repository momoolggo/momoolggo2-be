package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("internal/review")
public class InternalReviewController {

    @PutMapping("/{reviewId}/action")
    public ResultResponse<Void> reviewAction(@PathVariable Long reviewId,
                                         @RequestBody Map<String, String> body) {
        //TODO: 리뷰 로직 구현 예정
        return new ResultResponse<>("리뷰 처리 완료", null);
    }
}
