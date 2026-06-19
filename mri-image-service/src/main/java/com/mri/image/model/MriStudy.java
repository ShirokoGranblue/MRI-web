package com.mri.image.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "MRI Study")
public record MriStudy(Long id, Long examOrderId, String studyInstanceUid, String description, String status) {
}
