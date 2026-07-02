package com.mri.report.client;

public interface ImageClient {
    String studyDescription(Long studyId);

    Long studyExamOrderId(Long studyId);
}
