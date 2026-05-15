package com.green.mmg.main.owner;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.owner.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owner/review")
@RequiredArgsConstructor
public class OwnerReviewController {
    private final OwnerReviewService ownerReviewService;

    @GetMapping
    public ResultResponse<OwnerReviewListRes> customerReviewViews(@AuthenticationPrincipal UserPrincipal principal,
                                                                  @ModelAttribute OwnerReviewReq req){
        return new ResultResponse<>("조회 성공",
                ownerReviewService.customerReviewViews(principal.getSignedUserNo(), req));
    }


    @PostMapping("/{reviewId}/report")
    public ResultResponse<Void> reportReview(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long reviewId,
                                             @RequestBody OwnerReviewReportReq req) {
        ownerReviewService.reportReview(principal.getSignedUserNo(), reviewId, req);
        return new ResultResponse<>("신고 완료", null);
    }

    @PostMapping("/{reviewId}/reply")
    public ResultResponse<Void> registerReviewReply(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long reviewId,
                                                    @RequestBody OwnerReviewReplyReq req) {
        ownerReviewService.registerReviewReply(principal.getSignedUserNo(), reviewId, req);
        return new ResultResponse<>("답글 등록 완료", null);
    }

    @PutMapping("/reply/{replyId}")
    public ResultResponse<Void> updateReviewReply(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long replyId,
                                                  @RequestBody OwnerReviewReplyUpdateReq req) {
        ownerReviewService.updateReviewReply(principal.getSignedUserNo(), replyId, req);
        return new ResultResponse<>("답글 수정 완료", null);
    }

    @DeleteMapping("/reply/{replyId}")
    public ResultResponse<Void> deleteReviewReply(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long replyId) {
        ownerReviewService.deleteReviewReply(principal.getSignedUserNo(), replyId);
        return new ResultResponse<>("답글 삭제 완료", null);
    }




}
