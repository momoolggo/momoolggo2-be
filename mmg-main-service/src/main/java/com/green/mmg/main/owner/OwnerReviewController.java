package com.green.mmg.main.owner;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.owner.model.OwnerReviewListRes;
import com.green.mmg.main.owner.model.OwnerReviewReq;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner/review")
@RequiredArgsConstructor
public class OwnerReviewController {
    private final OwnerReviewService ownerReviewService;

    @GetMapping
    public ResultResponse<OwnerReviewListRes> customerReviewViews(@ModelAttribute OwnerReviewReq req){
        return new ResultResponse<>("조회 성공", ownerReviewService.customerReviewViews(req));
    }

}
