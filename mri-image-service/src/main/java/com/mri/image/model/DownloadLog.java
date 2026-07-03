package com.mri.image.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "影像下载审计")
public record DownloadLog(Long id, Long studyId, Long fileId, String downloadType,
                          String operator, String reason, LocalDateTime downloadedAt) {
    public DownloadLog(Long id, Long studyId, String operator, String reason, LocalDateTime downloadedAt) {
        this(id, studyId, null, "LEGACY", operator, reason, downloadedAt);
    }
}
