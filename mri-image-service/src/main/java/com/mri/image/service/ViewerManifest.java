package com.mri.image.service;

import com.mri.image.model.ImageFile;
import com.mri.image.model.MriStudy;

import java.util.List;

public record ViewerManifest(MriStudy study, String watermark, boolean downloadEnabled, List<SeriesManifest> series) {
    public record SeriesManifest(Long seriesId, String seriesName, String bodyPosition, List<ImageFile> files) {
    }
}
