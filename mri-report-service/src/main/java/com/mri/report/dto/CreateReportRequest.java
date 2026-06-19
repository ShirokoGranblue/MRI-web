package com.mri.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建报告请求")
public record CreateReportRequest(Long examOrderId, Long studyId, String findings, String impression) {
}
