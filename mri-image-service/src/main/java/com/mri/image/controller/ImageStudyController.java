package com.mri.image.controller;

import com.mri.common.api.ApiResult;
import com.mri.common.api.PageResult;
import com.mri.image.config.ImageDemoProperties;
import com.mri.image.dto.ArchiveStudyRequest;
import com.mri.image.dto.PatientStudyView;
import com.mri.image.model.DownloadLog;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriSeries;
import com.mri.image.model.MriStudy;
import com.mri.image.repository.ImageStudyRepository;
import com.mri.image.service.ImageStudyService;
import com.mri.image.service.PatientImageQueryService;
import com.mri.image.service.ViewerManifest;
import com.mri.image.storage.MinioImageStorage.LoadedObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "MRI 影像接口", description = "Study、Series、Image 文件、viewer manifest、下载审计")
@RestController
@RequestMapping("/images")
public class ImageStudyController {
    private final ImageStudyService service;
    private final ImageStudyRepository repository;
    private final ImageDemoProperties properties;
    private final PatientImageQueryService patientQuery;

    public ImageStudyController(ImageStudyService service, ImageStudyRepository repository, ImageDemoProperties properties,
                                PatientImageQueryService patientQuery) {
        this.service = service;
        this.repository = repository;
        this.properties = properties;
        this.patientQuery = patientQuery;
    }

    @Operation(summary = "当前患者本人影像进度")
    @GetMapping("/mine/studies")
    public ApiResult<List<PatientStudyView>> myStudies(@RequestHeader("X-Authenticated-User") String username) {
        return ApiResult.ok(patientQuery.findMine(username));
    }

    @Operation(summary = "当前患者已发布影像预览清单")
    @GetMapping("/mine/studies/{studyId}/viewer-manifest")
    public ApiResult<ViewerManifest> myViewerManifest(@PathVariable Long studyId,
                                                      @RequestHeader("X-Authenticated-User") String username) {
        patientQuery.assertStudyVisible(studyId, username);
        return ApiResult.ok(service.viewerManifest(studyId, properties.getWatermark(), false));
    }

    @Operation(summary = "当前患者已发布影像文件内容")
    @GetMapping("/mine/files/{id}/content")
    public ResponseEntity<byte[]> myFileContent(@PathVariable Long id,
                                                @RequestHeader("X-Authenticated-User") String username) {
        patientQuery.assertFileVisible(id, username);
        LoadedObject obj = service.streamFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(obj.contentType()))
                .body(obj.content());
    }

    @Operation(summary = "Study 归档")
    @PostMapping("/studies")
    public ApiResult<MriStudy> archive(@RequestBody ArchiveStudyRequest request) {
        return ApiResult.ok(service.archive(request));
    }

    @Operation(summary = "Study 删除")
    @DeleteMapping("/studies/{id}")
    public ApiResult<Void> deleteStudy(@PathVariable Long id) {
        service.deleteStudy(id);
        return ApiResult.ok();
    }

    @Operation(summary = "Study 修改")
    @PutMapping("/studies/{id}")
    public ApiResult<MriStudy> updateStudy(@PathVariable Long id, @RequestBody MriStudy study) {
        return ApiResult.ok(service.updateStudy(new MriStudy(id, study.examOrderId(), study.studyInstanceUid(), study.description(), study.status())));
    }

    @Operation(summary = "Study 详情")
    @GetMapping("/studies/{id}")
    public ApiResult<MriStudy> studyDetail(@PathVariable Long id) {
        return ApiResult.ok(service.findStudy(id));
    }

    @Operation(summary = "Study 分页查询")
    @GetMapping("/studies")
    public ApiResult<PageResult<MriStudy>> pageStudies(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) String keyword) {
        return ApiResult.ok(repository.pageStudies(page, size, keyword));
    }

    @Operation(summary = "Series 新增")
    @PostMapping("/studies/{studyId}/series")
    public ApiResult<MriSeries> createSeries(@PathVariable Long studyId, @RequestBody MriSeries series) {
        return ApiResult.ok(service.createSeries(new MriSeries(null, studyId, series.seriesName(), series.bodyPosition())));
    }

    @Operation(summary = "Series 删除")
    @DeleteMapping("/series/{id}")
    public ApiResult<Void> deleteSeries(@PathVariable Long id) {
        service.deleteSeries(id);
        return ApiResult.ok();
    }

    @Operation(summary = "Series 修改")
    @PutMapping("/series/{id}")
    public ApiResult<MriSeries> updateSeries(@PathVariable Long id, @RequestBody MriSeries series) {
        return ApiResult.ok(service.updateSeries(new MriSeries(id, series.studyId(), series.seriesName(), series.bodyPosition())));
    }

    @Operation(summary = "Series 详情")
    @GetMapping("/series/{id}")
    public ApiResult<MriSeries> seriesDetail(@PathVariable Long id) {
        return ApiResult.ok(repository.findSeries(id).orElseThrow(() -> new IllegalArgumentException("Series 不存在")));
    }

    @Operation(summary = "Series 列表")
    @GetMapping("/studies/{studyId}/series")
    public ApiResult<List<MriSeries>> listSeries(@PathVariable Long studyId) {
        return ApiResult.ok(repository.findSeriesByStudyId(studyId));
    }

    @Operation(summary = "影像文件上传登记")
    @PostMapping(value = "/studies/{studyId}/files", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<ImageFile> uploadFile(@PathVariable Long studyId, @RequestBody ImageFile file) {
        String storagePath = file.storagePath() == null ? "storage/mri-images/" + file.fileName() : file.storagePath();
        return ApiResult.ok(service.createFile(new ImageFile(null, file.seriesId(), file.fileName(), storagePath, file.checksum()), studyId));
    }

    @Operation(summary = "上传影像文件到对象存储")
    @PostMapping(value = "/studies/{studyId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<ImageFile> uploadToStorage(@PathVariable("studyId") Long studyId,
                                                @RequestParam("seriesId") Long seriesId,
                                                @RequestPart("file") MultipartFile file) {
        return ApiResult.ok(service.uploadFile(seriesId, file));
    }

    @Operation(summary = "影像文件内容")
    @GetMapping("/files/{id}/content")
    public ResponseEntity<byte[]> fileContent(@PathVariable Long id) {
        LoadedObject obj = service.streamFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(obj.contentType()))
                .body(obj.content());
    }

    @Operation(summary = "影像文件详情")
    @GetMapping("/files/{id}")
    public ApiResult<ImageFile> fileDetail(@PathVariable Long id) {
        return ApiResult.ok(repository.findFile(id).orElseThrow(() -> new IllegalArgumentException("影像文件不存在")));
    }

    @Operation(summary = "影像文件删除")
    @DeleteMapping("/files/{id}")
    public ApiResult<Void> deleteFile(@PathVariable Long id) {
        service.deleteFile(id);
        return ApiResult.ok();
    }

    @Operation(summary = "影像预览清单")
    @GetMapping("/studies/{studyId}/viewer-manifest")
    public ApiResult<ViewerManifest> viewerManifest(@PathVariable Long studyId) {
        return ApiResult.ok(service.viewerManifest(studyId, properties.getWatermark(), properties.isDownloadEnabled()));
    }

    @Operation(summary = "下载影像")
    @PostMapping("/studies/{studyId}/download")
    public ApiResult<DownloadLog> download(@PathVariable Long studyId,
                                           @RequestParam(defaultValue = "demo-user") String operator,
                                           @RequestParam(defaultValue = "教学演示下载") String reason) {
        if (!properties.isDownloadEnabled()) {
            throw new IllegalArgumentException("当前配置已关闭影像下载");
        }
        return ApiResult.ok(repository.createDownloadLog(new DownloadLog(null, studyId, operator, reason, LocalDateTime.now())));
    }

    @Operation(summary = "下载记录查询")
    @GetMapping("/studies/{studyId}/download-logs")
    public ApiResult<List<DownloadLog>> downloadLogs(@PathVariable Long studyId) {
        return ApiResult.ok(repository.downloadLogs(studyId));
    }

    @Operation(summary = "影像缓存演示查询")
    @GetMapping("/studies/{studyId}/cache-demo")
    public ApiResult<Map<String, Object>> cacheDemo(@PathVariable Long studyId) {
        MriStudy study = service.findStudy(studyId);
        ViewerManifest manifest = service.viewerManifest(studyId, properties.getWatermark(), properties.isDownloadEnabled());
        return ApiResult.ok(Map.of("study", study, "seriesCount", manifest.series().size(), "watermark", properties.getWatermark()));
    }

    @Operation(summary = "动态配置查询")
    @GetMapping("/demo/config")
    public ApiResult<Map<String, Object>> demoConfig() {
        return ApiResult.ok(Map.of("watermark", properties.getWatermark(), "downloadEnabled", properties.isDownloadEnabled()));
    }
}
