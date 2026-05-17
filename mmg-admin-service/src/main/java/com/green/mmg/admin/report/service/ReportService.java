package com.green.mmg.admin.report.service;

import com.green.mmg.admin.report.dto.ReportReq;
import com.green.mmg.admin.report.entity.Report;
import com.green.mmg.admin.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    @Transactional
    public void reportReview(ReportReq req) {
        Report report = new Report(
                req.getReporterNo(),
                req.getReviewId(),
                req.getReason(),
                req.getContent()
        );
        reportRepository.save(report);
    }
}
