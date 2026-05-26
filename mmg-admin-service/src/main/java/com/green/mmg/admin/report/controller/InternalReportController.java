package com.green.mmg.admin.report.controller;

import com.green.mmg.admin.report.dto.ReportReq;
import com.green.mmg.admin.report.service.ReportService;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/report")
@RequiredArgsConstructor
public class InternalReportController {

    private final ReportService reportService;

    @PostMapping("/review")
    public ResultResponse<?> reportReview(@RequestBody ReportReq req) {
        reportService.reportReview(req);
        return new ResultResponse<>("신고 접수 완료", null);
    }
}