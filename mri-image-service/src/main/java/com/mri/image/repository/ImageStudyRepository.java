package com.mri.image.repository;

import com.mri.common.api.PageResult;
import com.mri.image.dto.ArchiveStudyRequest;
import com.mri.image.model.DownloadLog;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriSeries;
import com.mri.image.model.MriStudy;

import java.util.List;
import java.util.Optional;

public interface ImageStudyRepository {
    MriStudy archive(ArchiveStudyRequest request);

    Optional<MriStudy> findStudyById(Long id);

    PageResult<MriStudy> pageStudies(long page, long size, String keyword);

    MriStudy updateStudy(MriStudy study);

    void deleteStudy(Long id);

    MriSeries createSeries(MriSeries series);

    MriSeries updateSeries(MriSeries series);

    Optional<MriSeries> findSeries(Long id);

    List<MriSeries> findSeriesByStudyId(Long studyId);

    void deleteSeries(Long id);

    ImageFile createFile(ImageFile file);

    Optional<ImageFile> findFile(Long id);

    List<ImageFile> findFilesByStudyId(Long studyId);

    List<ImageFile> findFilesBySeriesId(Long seriesId);

    void deleteFilesByStudyId(Long studyId);

    void deleteFilesBySeriesId(Long seriesId);

    void deleteSeriesByStudyId(Long studyId);

    void deleteFile(Long id);

    DownloadLog createDownloadLog(DownloadLog log);

    List<DownloadLog> downloadLogs(Long studyId);
}
