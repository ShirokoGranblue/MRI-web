package com.mri.patient.client;

import java.time.LocalDateTime;

public record ExamSummary(Long patientId, String examItem, String status, LocalDateTime createdAt) {
}
