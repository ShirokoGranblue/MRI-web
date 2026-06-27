package com.mri.image.dto;

import com.mri.image.model.MriStudy;

public record PatientStudyView(MriStudy study, int fileCount, boolean reportPublished) {
}
