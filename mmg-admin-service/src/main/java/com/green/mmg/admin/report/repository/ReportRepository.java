package com.green.mmg.admin.report.repository;

import com.green.mmg.admin.report.entity.AiStatus;
import com.green.mmg.admin.report.entity.Report;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByAiStatusAndAiRetryCountLessThan(AiStatus aiStatus, int maxRetry);

    boolean existsByReporterNoAndTargetTypeAndTargetNo(Long reporterNo, String targetType, Long targetNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Report> findFirstByTargetNoOrderByReportIdAsc(Long targetNo);
}
