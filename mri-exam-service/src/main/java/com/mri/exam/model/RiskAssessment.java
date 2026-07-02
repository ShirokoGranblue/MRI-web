package com.mri.exam.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "MRI 检查安全风险评估")
public record RiskAssessment(
        String level,
        String summary,
        LocalDateTime evaluatedAt,
        List<PatientContraindication> items
) {
}
