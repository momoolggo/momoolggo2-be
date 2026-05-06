package com.green.mmg.admin.notice.controller;

import com.green.mmg.admin.notice.dto.NoticeReq;
import com.green.mmg.admin.notice.service.NoticeService;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // 공지 목록 조회
    @GetMapping
    public ResultResponse<?> getNoticeList() {
        return new ResultResponse<>("조회 성공", noticeService.getNoticeList());
    }

    // 공지 등록
    @PostMapping
    public ResultResponse<?> createNotice(@RequestBody NoticeReq req) {
        noticeService.createNotice(req);
        return new ResultResponse<>("등록 완료", null);
    }

    // 공지 수정
    @PutMapping("/{noticeId}")
    public ResultResponse<?> updateNotice(@PathVariable Long noticeId,
                                          @RequestBody NoticeReq req) {
        noticeService.updateNotice(noticeId, req);
        return new ResultResponse<>("수정 완료", null);
    }

    // 공지 삭제
    @DeleteMapping("/{noticeId}")
    public ResultResponse<?> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return new ResultResponse<>("삭제 완료", null);
    }
}