package com.mri.image.service;

import com.mri.image.config.ImageDemoProperties;
import com.mri.image.model.DownloadLog;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriSeries;
import com.mri.image.repository.ImageStudyRepository;
import com.mri.image.storage.MinioImageStorage;
import com.mri.image.storage.MinioImageStorage.LoadedObject;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ImageDownloadService {
    private static final Set<String> DOCTOR_REASONS = Set.of("诊断查看", "会诊", "归档导出");
    private final ImageStudyRepository repository;
    private final MinioImageStorage storage;
    private final PatientImageQueryService patientQuery;
    private final ImageDemoProperties properties;

    public ImageDownloadService(ImageStudyRepository repository, MinioImageStorage storage,
                                PatientImageQueryService patientQuery, ImageDemoProperties properties) {
        this.repository = repository;
        this.storage = storage;
        this.patientQuery = patientQuery;
        this.properties = properties;
    }

    public DownloadedFile downloadDoctorFile(Long fileId, String operator, String reason) {
        requireDownloadEnabled();
        String trustedOperator = requireOperator(operator);
        String validatedReason = validateDoctorReason(reason);
        return downloadFile(fileId, trustedOperator, validatedReason);
    }

    public DownloadedFile downloadPatientFile(Long fileId, String username) {
        requireDownloadEnabled();
        String operator = requireOperator(username);
        patientQuery.assertFileVisible(fileId, operator);
        return downloadFile(fileId, operator, "患者本人下载");
    }

    public DownloadArchive downloadDoctorStudy(Long studyId, String operator, String reason) {
        requireDownloadEnabled();
        return downloadStudy(studyId, requireOperator(operator), validateDoctorReason(reason));
    }

    public DownloadArchive downloadPatientStudy(Long studyId, String username) {
        requireDownloadEnabled();
        String operator = requireOperator(username);
        patientQuery.assertStudyVisible(studyId, operator);
        return downloadStudy(studyId, operator, "患者本人下载");
    }

    private DownloadedFile downloadFile(Long fileId, String operator, String reason) {
        FileContext context = requireFileContext(fileId);
        LoadedObject object = storage.loadObject(context.file().storagePath());
        repository.createDownloadLog(new DownloadLog(
                null,
                context.studyId(),
                context.file().id(),
                "SINGLE",
                operator,
                reason,
                null
        ));
        return new DownloadedFile(
                safeFileName(context.file().fileName(), context.file().id()),
                object.contentType(),
                object.content()
        );
    }

    private DownloadArchive downloadStudy(Long studyId, String operator, String reason) {
        repository.findStudyById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("Study 不存在"));
        List<ImageFile> files = repository.findFilesByStudyId(studyId);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("该 Study 没有可下载的影像文件");
        }
        StreamingResponseBody body = output -> writeZip(output, studyId, files, operator, reason);
        return new DownloadArchive("Study-" + studyId + "-影像.zip", body);
    }

    private void writeZip(OutputStream output, Long studyId, List<ImageFile> files,
                          String operator, String reason) {
        ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8);
        Set<String> usedEntries = new HashSet<>();
        for (ImageFile file : files) {
            String entryName = uniqueEntryName(usedEntries, file);
            try {
                zip.putNextEntry(new ZipEntry(entryName));
                storage.writeObject(file.storagePath(), zip);
                zip.closeEntry();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IllegalStateException("生成 Study 影像压缩包失败", ex);
            }
        }
        try {
            zip.finish();
            zip.flush();
        } catch (Exception ex) {
            throw new IllegalStateException("生成 Study 影像压缩包失败", ex);
        }
        repository.createDownloadLog(new DownloadLog(
                null,
                studyId,
                null,
                "STUDY_ZIP",
                operator,
                reason,
                null
        ));
    }

    private FileContext requireFileContext(Long fileId) {
        ImageFile file = repository.findFile(fileId)
                .orElseThrow(() -> new IllegalArgumentException("影像文件不存在"));
        MriSeries series = repository.findSeries(file.seriesId())
                .orElseThrow(() -> new IllegalArgumentException("影像所属 Series 不存在"));
        repository.findStudyById(series.studyId())
                .orElseThrow(() -> new IllegalArgumentException("影像所属 Study 不存在"));
        return new FileContext(file, series.studyId());
    }

    private String uniqueEntryName(Set<String> usedEntries, ImageFile file) {
        String baseName = safeFileName(file.fileName(), file.id());
        String prefix = "series-" + file.seriesId() + "/";
        String candidate = prefix + baseName;
        int suffix = 2;
        while (!usedEntries.add(candidate)) {
            int dot = baseName.lastIndexOf('.');
            String stem = dot > 0 ? baseName.substring(0, dot) : baseName;
            String extension = dot > 0 ? baseName.substring(dot) : "";
            candidate = prefix + stem + "-" + suffix++ + extension;
        }
        return candidate;
    }

    private String safeFileName(String fileName, Long fileId) {
        String value = fileName == null || fileName.isBlank() ? "image-" + fileId + ".bin" : fileName.trim();
        value = value.replaceAll("[\\\\/:*?\"<>|]", "_");
        while (value.contains("..")) {
            value = value.replace("..", "_");
        }
        return value.isBlank() ? "image-" + fileId + ".bin" : value;
    }

    private String validateDoctorReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("请选择下载用途");
        }
        String value = reason.trim();
        if (DOCTOR_REASONS.contains(value)) {
            return value;
        }
        if (value.startsWith("其他：") && value.length() > 3) {
            return value;
        }
        throw new IllegalArgumentException("选择“其他”用途时必须填写说明");
    }

    private String requireOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            throw new IllegalArgumentException("无法确认当前登录用户");
        }
        return operator.trim();
    }

    private void requireDownloadEnabled() {
        if (!properties.isDownloadEnabled()) {
            throw new IllegalArgumentException("当前配置已关闭影像下载");
        }
    }

    public record DownloadedFile(String fileName, String contentType, byte[] content) {
    }

    public record DownloadArchive(String fileName, StreamingResponseBody body) {
    }

    private record FileContext(ImageFile file, Long studyId) {
    }
}
