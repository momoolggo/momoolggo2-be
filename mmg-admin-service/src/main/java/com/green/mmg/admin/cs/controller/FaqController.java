package com.green.mmg.admin.cs.controller;

import com.green.mmg.admin.common.enums.FaqCategory;
import com.green.mmg.admin.cs.dto.FaqReq;
import com.green.mmg.admin.cs.service.FaqService;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cs/faq")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    // FAQ 목록 조회
    @GetMapping
    public ResultResponse<?> getFaqList(
            @RequestParam(required = false) FaqCategory type) {
        return new ResultResponse<>("조회 성공", faqService.getFaqList(type));
    }

    // FAQ 등록
    @PostMapping
    public ResultResponse<?> createFaq(@RequestBody FaqReq req) {
        faqService.createFaq(req);
        return new ResultResponse<>("등록 완료", null);
    }

    // FAQ 수정
    @PutMapping("/{faqId}")
    public ResultResponse<?> updateFaq(@PathVariable Long faqId,
                                       @RequestBody FaqReq req) {
        faqService.updateFaq(faqId, req);
        return new ResultResponse<>("수정 완료", null);
    }

    // FAQ 삭제
    @DeleteMapping("/{faqId}")
    public ResultResponse<?> deleteFaq(@PathVariable Long faqId) {
        faqService.deleteFaq(faqId);
        return new ResultResponse<>("삭제 완료", null);
    }

    // 노출여부 토글
    @PatchMapping("/{faqId}/visible")
    public ResultResponse<?> toggleVisible(@PathVariable Long faqId) {
        faqService.toggleVisible(faqId);
        return new ResultResponse<>("노출여부 변경 완료", null);
    }
}