package com.green.mmg.admin.blind.controller;

import com.green.mmg.admin.blind.dto.BlindReq;
import com.green.mmg.admin.blind.service.BlindService;
import com.green.mmg.admin.common.enums.BlindStatus;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/blind")
@RequiredArgsConstructor
public class BlindController {

    private final BlindService blindService;

    // 블라인드 목록 조회
    @GetMapping
    public ResultResponse<?> getBlindList(
            @RequestParam(required = false) BlindStatus status) {
        return new ResultResponse<>("조회 성공", blindService.getBlindList(status));
    }

    // 블라인드 상세 조회
    @GetMapping("/{blindId}")
    public ResultResponse<?> getBlindDetail(@PathVariable Long blindId) {
        return new ResultResponse<>("조회 성공", blindService.getBlindDetail(blindId));
    }

    // 블라인드 처리 (사장 신고 시)
    @PostMapping
    public ResultResponse<?> blindReview(@RequestBody BlindReq req) {
        blindService.blindReview(req);
        return new ResultResponse<>("블라인드 처리 완료", null);
    }

    // 블라인드 확정 (관리자 검토 후 REVIEWING → BLINDED)
    @PatchMapping("/{blindId}/confirm")
    public ResultResponse<?> confirmBlind(@PathVariable Long blindId) {
        blindService.confirmBlind(blindId);
        return new ResultResponse<>("블라인드 확정 완료", null);
    }

    // 블라인드 해제 (관리자 검토 후 REVIEWING → RELEASED)
    @PatchMapping("/{blindId}/release")
    public ResultResponse<?> releaseBlind(@PathVariable Long blindId) {
        blindService.releaseBlind(blindId);
        return new ResultResponse<>("블라인드 해제 완료", null);
    }

    // 계정 15일 정지
    @PatchMapping("/{blindId}/suspend")
    public ResultResponse<?> suspendUser(@PathVariable Long blindId) {
        blindService.suspendUser(blindId);
        return new ResultResponse<>("계정 정지 완료", null);
    }

    // 영구 정지
    @PatchMapping("/{blindId}/permanent")
    public ResultResponse<?> permanentSuspend(@PathVariable Long blindId) {
        blindService.permanentSuspend(blindId);
        return new ResultResponse<>("영구 정지 완료", null);
    }
}