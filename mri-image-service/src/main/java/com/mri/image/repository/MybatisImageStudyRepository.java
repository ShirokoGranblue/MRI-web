package com.mri.image.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mri.common.api.PageResult;
import com.mri.image.dto.ArchiveStudyRequest;
import com.mri.image.entity.DownloadLogEntity;
import com.mri.image.entity.ImageFileEntity;
import com.mri.image.entity.MriSeriesEntity;
import com.mri.image.entity.MriStudyEntity;
import com.mri.image.mapper.DownloadLogMapper;
import com.mri.image.mapper.ImageFileMapper;
import com.mri.image.mapper.MriSeriesMapper;
import com.mri.image.mapper.MriStudyMapper;
import com.mri.image.model.DownloadLog;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriSeries;
import com.mri.image.model.MriStudy;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class MybatisImageStudyRepository implements ImageStudyRepository {
    private final MriStudyMapper studyMapper;
    private final MriSeriesMapper seriesMapper;
    private final ImageFileMapper fileMapper;
    private final DownloadLogMapper downloadLogMapper;

    public MybatisImageStudyRepository(MriStudyMapper studyMapper, MriSeriesMapper seriesMapper, ImageFileMapper fileMapper, DownloadLogMapper downloadLogMapper) {
        this.studyMapper = studyMapper;
        this.seriesMapper = seriesMapper;
        this.fileMapper = fileMapper;
        this.downloadLogMapper = downloadLogMapper;
    }

    @Override
    public MriStudy archive(ArchiveStudyRequest request) {
        MriStudyEntity entity = new MriStudyEntity();
        entity.setExamOrderId(request.examOrderId());
        entity.setStudyInstanceUid(request.studyInstanceUid());
        entity.setDescription(request.description());
        entity.setStatus("ARCHIVED");
        studyMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public Optional<MriStudy> findStudyById(Long id) {
        return Optional.ofNullable(studyMapper.selectById(id)).map(MybatisImageStudyRepository::toModel);
    }

    @Override
    public PageResult<MriStudy> pageStudies(long page, long size, String keyword) {
        LambdaQueryWrapper<MriStudyEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(MriStudyEntity::getDescription, keyword).or().like(MriStudyEntity::getStudyInstanceUid, keyword);
        }
        Page<MriStudyEntity> result = studyMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(page, size, result.getTotal(), result.getRecords().stream().map(MybatisImageStudyRepository::toModel).toList());
    }

    @Override
    public MriStudy updateStudy(MriStudy study) {
        studyMapper.updateById(toEntity(study));
        return study;
    }

    @Override
    public void deleteStudy(Long id) {
        studyMapper.deleteById(id);
    }

    @Override
    public MriSeries createSeries(MriSeries series) {
        MriSeriesEntity entity = toEntity(series);
        seriesMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public MriSeries updateSeries(MriSeries series) {
        seriesMapper.updateById(toEntity(series));
        return series;
    }

    @Override
    public Optional<MriSeries> findSeries(Long id) {
        return Optional.ofNullable(seriesMapper.selectById(id)).map(MybatisImageStudyRepository::toModel);
    }

    @Override
    public List<MriSeries> findSeriesByStudyId(Long studyId) {
        return seriesMapper.selectList(new LambdaQueryWrapper<MriSeriesEntity>().eq(MriSeriesEntity::getStudyId, studyId))
                .stream().map(MybatisImageStudyRepository::toModel).toList();
    }

    @Override
    public void deleteSeries(Long id) {
        seriesMapper.deleteById(id);
    }

    @Override
    public ImageFile createFile(ImageFile file) {
        ImageFileEntity entity = toEntity(file);
        fileMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public Optional<ImageFile> findFile(Long id) {
        return Optional.ofNullable(fileMapper.selectById(id)).map(MybatisImageStudyRepository::toModel);
    }

    @Override
    public List<ImageFile> findFilesByStudyId(Long studyId) {
        Set<Long> seriesIds = findSeriesByStudyId(studyId).stream().map(MriSeries::id).collect(java.util.stream.Collectors.toSet());
        if (seriesIds.isEmpty()) {
            return List.of();
        }
        return fileMapper.selectList(new LambdaQueryWrapper<ImageFileEntity>().in(ImageFileEntity::getSeriesId, seriesIds))
                .stream().map(MybatisImageStudyRepository::toModel).toList();
    }

    @Override
    public void deleteFile(Long id) {
        fileMapper.deleteById(id);
    }

    @Override
    public DownloadLog createDownloadLog(DownloadLog log) {
        DownloadLogEntity entity = toEntity(log);
        entity.setDownloadedAt(LocalDateTime.now());
        downloadLogMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public List<DownloadLog> downloadLogs(Long studyId) {
        return downloadLogMapper.selectList(new LambdaQueryWrapper<DownloadLogEntity>().eq(DownloadLogEntity::getStudyId, studyId))
                .stream().map(MybatisImageStudyRepository::toModel).toList();
    }

    private static MriStudy toModel(MriStudyEntity entity) {
        return new MriStudy(entity.getId(), entity.getExamOrderId(), entity.getStudyInstanceUid(), entity.getDescription(), entity.getStatus());
    }

    private static MriStudyEntity toEntity(MriStudy study) {
        MriStudyEntity entity = new MriStudyEntity();
        entity.setId(study.id());
        entity.setExamOrderId(study.examOrderId());
        entity.setStudyInstanceUid(study.studyInstanceUid());
        entity.setDescription(study.description());
        entity.setStatus(study.status());
        return entity;
    }

    private static MriSeries toModel(MriSeriesEntity entity) {
        return new MriSeries(entity.getId(), entity.getStudyId(), entity.getSeriesName(), entity.getBodyPosition());
    }

    private static MriSeriesEntity toEntity(MriSeries series) {
        MriSeriesEntity entity = new MriSeriesEntity();
        entity.setId(series.id());
        entity.setStudyId(series.studyId());
        entity.setSeriesName(series.seriesName());
        entity.setBodyPosition(series.bodyPosition());
        return entity;
    }

    private static ImageFile toModel(ImageFileEntity entity) {
        return new ImageFile(entity.getId(), entity.getSeriesId(), entity.getFileName(), entity.getStoragePath(), entity.getChecksum());
    }

    private static ImageFileEntity toEntity(ImageFile file) {
        ImageFileEntity entity = new ImageFileEntity();
        entity.setId(file.id());
        entity.setSeriesId(file.seriesId());
        entity.setFileName(file.fileName());
        entity.setStoragePath(file.storagePath());
        entity.setChecksum(file.checksum());
        return entity;
    }

    private static DownloadLog toModel(DownloadLogEntity entity) {
        return new DownloadLog(entity.getId(), entity.getStudyId(), entity.getOperator(), entity.getReason(), entity.getDownloadedAt());
    }

    private static DownloadLogEntity toEntity(DownloadLog log) {
        DownloadLogEntity entity = new DownloadLogEntity();
        entity.setId(log.id());
        entity.setStudyId(log.studyId());
        entity.setOperator(log.operator());
        entity.setReason(log.reason());
        entity.setDownloadedAt(log.downloadedAt());
        return entity;
    }
}
