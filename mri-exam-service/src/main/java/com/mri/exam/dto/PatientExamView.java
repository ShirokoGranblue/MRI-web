package com.mri.exam.dto;

import com.mri.exam.model.ExamOrder;
import com.mri.exam.model.MriSchedule;

import java.time.LocalDateTime;
import java.util.List;

public record PatientExamView(
        Long id,
        String examItem,
        String clinicalDiagnosis,
        String priority,
        String status,
        LocalDateTime createdAt,
        List<MriSchedule> schedules
) {
    public static PatientExamView from(ExamOrder exam, List<MriSchedule> schedules) {
        return new PatientExamView(
                exam.id(),
                exam.examItem(),
                exam.clinicalDiagnosis(),
                exam.priority(),
                exam.status(),
                exam.createdAt(),
                schedules
        );
    }
}
