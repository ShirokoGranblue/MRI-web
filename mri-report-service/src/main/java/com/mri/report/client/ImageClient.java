package com.mri.report.client;

public interface ImageClient {
    String studyDescription(Long studyId);

    boolean studyExists(Long studyId);
}
