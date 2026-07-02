package com.mri.exam.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "患者 MRI 禁忌症风险明细")
public record PatientContraindication(
        Long id,
        Long patientId,
        String type,
        String description,
        String severity
) {
}
