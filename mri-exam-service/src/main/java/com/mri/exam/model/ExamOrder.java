package com.mri.exam.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "MRI 检查申请单")
public record ExamOrder(Long id, Long patientId, String examItem, String status) {
}
