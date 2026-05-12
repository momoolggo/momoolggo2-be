package com.green.mmg.rider.notice;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.rider.notice.dto.RiderNoticeRowRes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * R9 라이더 공지 controller — interfaces.md No.90 / REQ-RDR-006.
 *
 * <p>R6 RiderOrderController 분리 패턴 일관 (decision-#31 (가)). RIDER role 인증 필수.
 * 작성/수정/삭제는 admin-service 진입점 (RiderInternalController.notice). 라이더 측은 GET만.</p>
 */
@RestController
@RequestMapping("/api/rider/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResultResponse<List<RiderNoticeRowRes>> getNotices(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<RiderNoticeRowRes> data = noticeService.getRiderNoticeList();
        return new ResultResponse<>("공지 목록 조회 성공", data);
    }
}
