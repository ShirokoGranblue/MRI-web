package com.mri.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "MRI 诊断报告")
public record Report(Long id, Long examOrderId, Long studyId, String findings, String status) {
}
