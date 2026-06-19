package com.mri.image.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "MRI 影像文件")
public record ImageFile(Long id, Long seriesId, String fileName, String storagePath, String checksum) {
}
