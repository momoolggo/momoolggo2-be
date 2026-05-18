package com.green.mmg.admin.report.repository;

import com.green.mmg.admin.report.entity.AiStatus;
import com.green.mmg.admin.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByAiStatusAndAiRetryCountLessThan(AiStatus aiStatus, int maxRetry);
}
