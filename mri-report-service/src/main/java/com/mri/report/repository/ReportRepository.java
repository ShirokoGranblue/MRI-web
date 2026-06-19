package com.mri.report.repository;

import com.mri.common.api.PageResult;
import com.mri.report.dto.CreateReportRequest;
import com.mri.report.model.Report;
import com.mri.report.model.ReportAuditLog;

import java.util.List;
import java.util.Optional;

public interface ReportRepository {
    Report createDraft(CreateReportRequest request);

    Optional<Report> findById(Long id);

    PageResult<Report> page(long page, long size, String status);

    Report update(Long id, CreateReportRequest request);

    void delete(Long id);

    Report updateStatus(Long id, String status);

    Optional<Report> findByExamOrderId(Long examOrderId);

    ReportAuditLog audit(Long reportId, String action, String operator, String comment);

    List<ReportAuditLog> auditLogs(Long reportId);
}
