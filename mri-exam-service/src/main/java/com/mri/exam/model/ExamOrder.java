package com.mri.exam.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "MRI 检查申请单")
public record ExamOrder(Long id, Long patientId, String examItem, String clinicalDiagnosis,
                        String priority, String status, LocalDateTime createdAt) {
}
