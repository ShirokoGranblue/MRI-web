package com.mri.exam.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "MRI 检查排程")
public record MriSchedule(Long id, Long examOrderId, String scannerRoom, LocalDateTime scheduledAt,
                          String technologist, Integer durationMinutes) {
    public MriSchedule(Long id, Long examOrderId, String scannerRoom, LocalDateTime scheduledAt, String technologist) {
        this(id, examOrderId, scannerRoom, scheduledAt, technologist, 30);
    }
}
