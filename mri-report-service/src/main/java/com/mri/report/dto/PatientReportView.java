package com.mri.report.dto;

import com.mri.report.entity.ReportEntity;

public record PatientReportView(
        Long id,
        Long examOrderId,
        Long studyId,
        String findings,
        String impression,
        String status
) {
    public static PatientReportView from(ReportEntity entity) {
        boolean published = "PUBLISHED".equals(entity.getStatus());
        return new PatientReportView(
                entity.getId(),
                entity.getExamOrderId(),
                entity.getStudyId(),
                published ? entity.getFindings() : null,
                published ? entity.getImpression() : null,
                entity.getStatus()
        );
    }
}
