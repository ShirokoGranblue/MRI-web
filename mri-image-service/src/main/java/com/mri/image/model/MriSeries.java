package com.mri.image.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "MRI Series")
public record MriSeries(Long id, Long studyId, String seriesName, String bodyPosition) {
}
