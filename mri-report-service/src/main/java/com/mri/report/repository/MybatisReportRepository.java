package com.mri.report.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mri.common.api.PageResult;
import com.mri.report.dto.CreateReportRequest;
import com.mri.report.entity.ReportAuditLogEntity;
import com.mri.report.entity.ReportEntity;
import com.mri.report.mapper.ReportAuditLogMapper;
import com.mri.report.mapper.ReportMapper;
import com.mri.report.model.Report;
import com.mri.report.model.ReportAuditLog;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisReportRepository implements ReportRepository {
    private final ReportMapper reportMapper;
    private final ReportAuditLogMapper auditLogMapper;

    public MybatisReportRepository(ReportMapper reportMapper, ReportAuditLogMapper auditLogMapper) {
        this.reportMapper = reportMapper;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public Report createDraft(CreateReportRequest request) {
        if (findByExamOrderId(request.examOrderId()).isPresent()) {
            throw new IllegalArgumentException("该检查申请已存在诊断报告");
        }
        ReportEntity entity = new ReportEntity();
        entity.setExamOrderId(request.examOrderId());
        entity.setStudyId(request.studyId());
        entity.setFindings(request.findings());
        entity.setImpression(request.impression());
        entity.setStatus("DRAFT");
        reportMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public Optional<Report> findById(Long id) {
        return Optional.ofNullable(reportMapper.selectById(id)).map(MybatisReportRepository::toModel);
    }

    @Override
    public PageResult<Report> page(long page, long size, String status) {
        LambdaQueryWrapper<ReportEntity> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(ReportEntity::getStatus, status);
        }
        Page<ReportEntity> result = reportMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(page, size, result.getTotal(), result.getRecords().stream().map(MybatisReportRepository::toModel).toList());
    }

    @Override
    public Report update(Long id, CreateReportRequest request) {
        ReportEntity entity = reportMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("报告不存在");
        }
        entity.setExamOrderId(request.examOrderId());
        entity.setStudyId(request.studyId());
        entity.setFindings(request.findings());
        entity.setImpression(request.impression());
        reportMapper.updateById(entity);
        return toModel(entity);
    }

    @Override
    public void delete(Long id) {
        reportMapper.deleteById(id);
    }

    @Override
    public Report updateStatus(Long id, String status) {
        ReportEntity entity = reportMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("报告不存在");
        }
        entity.setStatus(status);
        reportMapper.updateById(entity);
        return toModel(entity);
    }

    @Override
    public Optional<Report> findByExamOrderId(Long examOrderId) {
        return reportMapper.selectList(new LambdaQueryWrapper<ReportEntity>()
                        .eq(ReportEntity::getExamOrderId, examOrderId)
                        .orderByDesc(ReportEntity::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(MybatisReportRepository::toModel);
    }

    @Override
    public ReportAuditLog audit(Long reportId, String action, String operator, String comment) {
        ReportAuditLogEntity entity = new ReportAuditLogEntity();
        entity.setReportId(reportId);
        entity.setAction(action);
        entity.setOperator(operator);
        entity.setComment(comment);
        entity.setOperatedAt(LocalDateTime.now());
        auditLogMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public List<ReportAuditLog> auditLogs(Long reportId) {
        return auditLogMapper.selectList(new LambdaQueryWrapper<ReportAuditLogEntity>().eq(ReportAuditLogEntity::getReportId, reportId))
                .stream().map(MybatisReportRepository::toModel).toList();
    }

    private static Report toModel(ReportEntity entity) {
        return new Report(entity.getId(), entity.getExamOrderId(), entity.getStudyId(), entity.getFindings(), entity.getStatus());
    }

    private static ReportAuditLog toModel(ReportAuditLogEntity entity) {
        return new ReportAuditLog(entity.getId(), entity.getReportId(), entity.getAction(), entity.getOperator(), entity.getComment(), entity.getOperatedAt());
    }
}
