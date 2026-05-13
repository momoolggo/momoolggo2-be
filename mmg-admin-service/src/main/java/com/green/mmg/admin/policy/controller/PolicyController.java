package com.green.mmg.admin.policy.controller;

import com.green.mmg.admin.policy.dto.PolicyReq;
import com.green.mmg.admin.policy.service.PolicyService;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/policy")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    // 정책 목록 조회
    @GetMapping
    public ResultResponse<?> getPolicyList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isActive) {
        return new ResultResponse<>("조회 성공",
                policyService.getPolicyList(type, isActive));
    }

    // 정책 등록
    @PostMapping
    public ResultResponse<?> createPolicy(@RequestBody PolicyReq req) {
        policyService.createPolicy(req);
        return new ResultResponse<>("등록 완료", null);
    }

    // 정책 수정
    @PutMapping("/{policyId}")
    public ResultResponse<?> updatePolicy(@PathVariable Long policyId,
                                          @RequestBody PolicyReq req) {
        policyService.updatePolicy(policyId, req);
        return new ResultResponse<>("수정 완료", null);
    }

    // 정책 비활성화
    @PutMapping("/{policyId}/deactivate")
    public ResultResponse<?> deactivatePolicy(@PathVariable Long policyId) {
        policyService.deactivatePolicy(policyId);
        return new ResultResponse<>("비활성화 완료", null);
    }
}