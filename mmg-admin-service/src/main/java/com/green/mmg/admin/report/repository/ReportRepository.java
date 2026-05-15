package com.green.mmg.admin.report.repository;

import com.green.mmg.admin.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
