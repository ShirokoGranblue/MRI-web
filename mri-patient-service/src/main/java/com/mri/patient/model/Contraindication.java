package com.mri.patient.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "MRI 禁忌症")
public record Contraindication(Long id, Long patientId, String type, String description, String severity) {
}
