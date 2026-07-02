package com.mri.image.service;

import com.mri.image.client.ExamClient;
import com.mri.image.dto.ArchiveStudyRequest;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriSeries;
import com.mri.image.model.MriStudy;
import com.mri.image.repository.ImageStudyRepository;
import com.mri.image.storage.MinioImageStorage;
import com.mri.image.storage.MinioImageStorage.LoadedObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImageStudyService {
    private final ImageStudyRepository repository;
    private final StudyCache cache;
    private final ExamClient examClient;
    private final MinioImageStorage storage;

    public ImageStudyService(ImageStudyRepository repository, StudyCache cache, ExamClient examClient, MinioImageStorage storage) {
        this.repository = repository;
        this.cache = cache;
        this.examClient = examClient;
        this.storage = storage;
    }

    public MriStudy archive(ArchiveStudyRequest request) {
        if (!examClient.examExists(request.examOrderId())) {
            throw new IllegalArgumentException("检查申请不存在，不能归档 MRI Study");
        }
        String status = examClient.examStatus(request.examOrderId());
        if (!"COMPLETED".equals(status)) {
            throw new IllegalArgumentException("检查申请尚未完成，不能归档影像");
        }
        MriStudy study = repository.archive(resolveStudyInstanceUid(request));
        cache.evict(study.id());
        return study;
    }

    private ArchiveStudyRequest resolveStudyInstanceUid(ArchiveStudyRequest request) {
        String uid = request.studyInstanceUid();
        if (uid != null && !uid.isBlank()) {
            return request;
        }
        return new ArchiveStudyRequest(request.examOrderId(), generateStudyInstanceUid(), request.description());
    }

    private String generateStudyInstanceUid() {
        return "1.2.840.113619." + UUID.randomUUID().toString().replace("-", "");
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
        for (ImageFile file : repository.findFilesByStudyId(id)) {
            storage.removeObjectQuietly(file.storagePath());
        }
        repository.deleteFilesByStudyId(id);
        repository.deleteSeriesByStudyId(id);
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
        for (ImageFile file : repository.findFilesBySeriesId(id)) {
            storage.removeObjectQuietly(file.storagePath());
        }
        repository.deleteFilesBySeriesId(id);
        repository.deleteSeries(id);
        cache.evict(existing.studyId());
    }

    public ImageFile createFile(ImageFile file, Long studyId) {
        requireSeriesInStudy(file.seriesId(), studyId);
        ImageFile created = repository.createFile(file);
        cache.evict(studyId);
        return created;
    }

    public ImageFile uploadFile(Long studyId, Long seriesId, MultipartFile file) {
        requireSeriesInStudy(seriesId, studyId);
        String objectKey = "series/" + seriesId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        try {
            storage.putObject(objectKey, file.getInputStream(), file.getSize(), resolveContentType(file));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取上传文件失败", e);
        }
        String checksum = sha256(file);
        ImageFile saved = repository.createFile(new ImageFile(null, seriesId, file.getOriginalFilename(), objectKey, checksum));
        cache.evict(studyId);
        return saved;
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType;
        }
        String inferred = URLConnection.guessContentTypeFromName(file.getOriginalFilename());
        return inferred == null ? "application/octet-stream" : inferred;
    }

    private void requireSeriesInStudy(Long seriesId, Long studyId) {
        MriSeries series = repository.findSeries(seriesId)
                .orElseThrow(() -> new IllegalArgumentException("Series 不存在"));
        if (!studyId.equals(series.studyId())) {
            throw new IllegalArgumentException("所选 Series 不属于该 Study");
        }
    }

    public LoadedObject streamFile(Long fileId) {
        ImageFile file = repository.findFile(fileId)
                .orElseThrow(() -> new IllegalArgumentException("影像文件不存在"));
        return storage.loadObject(file.storagePath());
    }

    public void deleteFile(Long id) {
        ImageFile existing = repository.findFile(id)
                .orElseThrow(() -> new IllegalArgumentException("影像文件不存在"));
        MriSeries series = repository.findSeries(existing.seriesId())
                .orElseThrow(() -> new IllegalArgumentException("Series 不存在"));
        storage.removeObjectQuietly(existing.storagePath());
        repository.deleteFile(id);
        cache.evict(series.studyId());
    }

    private String sha256(MultipartFile file) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(file.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
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
