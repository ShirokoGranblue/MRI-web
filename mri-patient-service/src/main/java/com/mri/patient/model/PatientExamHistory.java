package com.mri.patient.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "患者 MRI 检查历史")
public record PatientExamHistory(Long patientId, String examItem, String status, LocalDateTime examTime) {
}
