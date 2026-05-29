package com.green.mmg.admin.report.service;

import com.green.mmg.admin.report.dto.ReportReq;
import com.green.mmg.admin.report.entity.Report;
import com.green.mmg.admin.report.event.ReviewReportSubmittedEvent;
import com.green.mmg.admin.report.repository.ReportRepository;
import com.green.mmg.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long reportReview(ReportReq req) {
        Report report = new Report(
                req.getReporterNo(),
                req.getReviewId(),
                req.getReason(),
                req.getContent()
        );
        if (reportRepository.existsByReporterNoAndTargetTypeAndTargetNo(
                report.getReporterNo(), report.getTargetType(), report.getTargetNo())) {
            throw new BusinessException("이미 신고한 리뷰입니다.", HttpStatus.CONFLICT);
        }

        if (req.getReviewContent() != null) {
            report.setReviewContent(req.getReviewContent());
        }
        try {
            reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("이미 신고한 리뷰입니다.", HttpStatus.CONFLICT);
        }
        eventPublisher.publishEvent(new ReviewReportSubmittedEvent(report.getReportId()));
        return report.getReportId();
    }
}
