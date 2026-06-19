package com.mri.image.service;

import com.mri.image.client.ExamClient;
import com.mri.image.dto.ArchiveStudyRequest;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriSeries;
import com.mri.image.model.MriStudy;
import com.mri.image.repository.ImageStudyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageStudyService {
    private final ImageStudyRepository repository;
    private final StudyCache cache;
    private final ExamClient examClient;

    public ImageStudyService(ImageStudyRepository repository, StudyCache cache, ExamClient examClient) {
        this.repository = repository;
        this.cache = cache;
        this.examClient = examClient;
    }

    public MriStudy archive(ArchiveStudyRequest request) {
        if (!examClient.examExists(request.examOrderId())) {
            throw new IllegalArgumentException("检查申请不存在，不能归档 MRI Study");
        }
        MriStudy study = repository.archive(request);
        cache.evict(study.id());
        return study;
    }

    public MriStudy findStudy(Long id) {
        return cache.getStudy(id).orElseGet(() -> {
            MriStudy study = repository.findStudyById(id).orElseThrow(() -> new IllegalArgumentException("Study 不存在"));
            cache.putStudy(study);
            return study;
        });
    }

    public MriStudy updateStudy(MriStudy study) {
        MriStudy updated = repository.updateStudy(study);
        cache.evict(study.id());
        return updated;
    }

    public void deleteStudy(Long id) {
        repository.deleteStudy(id);
        cache.evict(id);
    }

    public MriSeries createSeries(MriSeries series) {
        MriSeries created = repository.createSeries(series);
        cache.evict(series.studyId());
        return created;
    }

    public MriSeries updateSeries(MriSeries series) {
        MriSeries existing = repository.findSeries(series.id())
                .orElseThrow(() -> new IllegalArgumentException("Series 不存在"));
        Long studyId = series.studyId() == null ? existing.studyId() : series.studyId();
        MriSeries updated = repository.updateSeries(new MriSeries(series.id(), studyId, series.seriesName(), series.bodyPosition()));
        cache.evict(studyId);
        return updated;
    }

    public void deleteSeries(Long id) {
        MriSeries existing = repository.findSeries(id)
                .orElseThrow(() -> new IllegalArgumentException("Series 不存在"));
        repository.deleteSeries(id);
        cache.evict(existing.studyId());
    }

    public ImageFile createFile(ImageFile file, Long studyId) {
        ImageFile created = repository.createFile(file);
        cache.evict(studyId);
        return created;
    }

    public void deleteFile(Long id) {
        ImageFile existing = repository.findFile(id)
                .orElseThrow(() -> new IllegalArgumentException("影像文件不存在"));
        MriSeries series = repository.findSeries(existing.seriesId())
                .orElseThrow(() -> new IllegalArgumentException("Series 不存在"));
        repository.deleteFile(id);
        cache.evict(series.studyId());
    }

    public ViewerManifest viewerManifest(Long studyId, String watermark, boolean downloadEnabled) {
        return cache.getManifest(studyId, watermark, downloadEnabled).orElseGet(() -> {
            MriStudy study = findStudy(studyId);
            List<MriSeries> series = repository.findSeriesByStudyId(studyId);
            Map<Long, List<ImageFile>> filesBySeries = repository.findFilesByStudyId(studyId).stream()
                    .collect(Collectors.groupingBy(ImageFile::seriesId));
            List<ViewerManifest.SeriesManifest> manifests = series.stream()
                    .map(item -> new ViewerManifest.SeriesManifest(
                            item.id(),
                            item.seriesName(),
                            item.bodyPosition(),
                            filesBySeries.getOrDefault(item.id(), List.of())))
                    .toList();
            ViewerManifest manifest = new ViewerManifest(study, watermark, downloadEnabled, manifests);
            cache.putManifest(studyId, manifest);
            return manifest;
        });
    }
}
