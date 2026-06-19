package com.mri.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "MRI Study 归档请求")
public record ArchiveStudyRequest(Long examOrderId, String studyInstanceUid, String description) {
}
