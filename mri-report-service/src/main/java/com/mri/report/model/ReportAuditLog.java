package com.mri.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "报告审核日志")
public record ReportAuditLog(Long id, Long reportId, String action, String operator, String comment, LocalDateTime operatedAt) {
}
