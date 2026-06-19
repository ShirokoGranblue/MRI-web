package com.mri.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建 MRI 检查申请")
public record CreateExamOrderRequest(Long patientId, String examItem, String clinicalDiagnosis, String priority) {
}
