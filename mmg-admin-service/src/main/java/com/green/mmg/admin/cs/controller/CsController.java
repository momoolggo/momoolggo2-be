package com.green.mmg.admin.cs.controller;

import com.green.mmg.admin.common.enums.InquiryStatus;
import com.green.mmg.admin.common.enums.InquiryUserType;
import com.green.mmg.admin.cs.dto.InquiryReq;
import com.green.mmg.admin.cs.service.CsService;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cs")
@RequiredArgsConstructor
public class CsController {

    private final CsService csService;

    // 문의 현황 카드
    @GetMapping("/summary")
    public ResultResponse<?> getSummary() {
        return new ResultResponse<>("조회 성공", csService.getSummary());
    }

    // 문의 목록 조회
    @GetMapping("/inquiry")
    public ResultResponse<?> getInquiryList(
            @RequestParam(required = false) InquiryUserType category,
            @RequestParam(required = false) InquiryStatus state) {
        return new ResultResponse<>("조회 성공", csService.getInquiryList(category, state));
    }

    // 문의 상세 조회
    @GetMapping("/inquiry/{inquiryId}")
    public ResultResponse<?> getInquiryDetail(@PathVariable Long inquiryId) {
        return new ResultResponse<>("조회 성공", csService.getInquiryDetail(inquiryId));
    }

    // 답변 등록
    @PostMapping("/inquiry/{inquiryId}/reply")
    public ResultResponse<?> replyInquiry(@PathVariable Long inquiryId,
                                          @RequestBody InquiryReq req) {
        csService.replyInquiry(inquiryId, req);
        return new ResultResponse<>("답변 등록 완료", null);
    }
}