package com.green.mmg.main.mypage;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/summary")
    public ResultResponse<MyPageSummaryRes> getSummary(@AuthenticationPrincipal UserPrincipal principal) {
        return new ResultResponse<>(
                "마이페이지 요약 조회 성공",
                myPageService.getSummary(principal.getSignedUserNo())
        );
    }
}
