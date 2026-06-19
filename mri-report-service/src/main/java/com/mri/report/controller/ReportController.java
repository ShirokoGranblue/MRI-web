package com.mri.report.controller;

import com.mri.common.api.ApiResult;
import com.mri.common.api.PageResult;
import com.mri.report.dto.CreateReportRequest;
import com.mri.report.model.Report;
import com.mri.report.model.ReportAuditLog;
import com.mri.report.repository.ReportRepository;
import com.mri.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "MRI 报告接口", description = "报告编写、审核、发布")
@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ReportService service;
    private final ReportRepository repository;

    public ReportController(ReportService service, ReportRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Operation(summary = "报告新增")
    @PostMapping
    public ApiResult<Report> create(@RequestBody CreateReportRequest request) {
        return ApiResult.ok(service.create(request));
    }

    @Operation(summary = "报告删除")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        repository.delete(id);
        return ApiResult.ok();
    }

    @Operation(summary = "报告修改")
    @PutMapping("/{id}")
    public ApiResult<Report> update(@PathVariable Long id, @RequestBody CreateReportRequest request) {
        return ApiResult.ok(repository.update(id, request));
    }

    @Operation(summary = "报告详情")
    @GetMapping("/{id}")
    public ApiResult<Report> detail(@PathVariable Long id) {
        return ApiResult.ok(repository.findById(id).orElseThrow(() -> new IllegalArgumentException("报告不存在")));
    }

    @Operation(summary = "报告分页查询")
    @GetMapping
    public ApiResult<PageResult<Report>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String status) {
        return ApiResult.ok(repository.page(page, size, status));
    }

    @Operation(summary = "提交审核")
    @PostMapping("/{id}/submit")
    public ApiResult<Report> submit(@PathVariable Long id) {
        return ApiResult.ok(service.submit(id));
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{id}/approve")
    public ApiResult<Report> approve(@PathVariable Long id) {
        return ApiResult.ok(service.approve(id));
    }

    @Operation(summary = "审核驳回")
    @PostMapping("/{id}/reject")
    public ApiResult<Report> reject(@PathVariable Long id, @RequestParam(defaultValue = "退回修改") String reason) {
        return ApiResult.ok(service.reject(id, reason));
    }

    @Operation(summary = "发布报告")
    @PostMapping("/{id}/publish")
    public ApiResult<Report> publish(@PathVariable Long id) {
        return ApiResult.ok(service.publish(id));
    }

    @Operation(summary = "按检查单查询报告")
    @GetMapping("/by-exam/{examOrderId}")
    public ApiResult<Report> byExam(@PathVariable Long examOrderId) {
        return ApiResult.ok(repository.findByExamOrderId(examOrderId).orElseThrow(() -> new IllegalArgumentException("报告不存在")));
    }

    @Operation(summary = "报告审核日志")
    @GetMapping("/{id}/audit-logs")
    public ApiResult<List<ReportAuditLog>> auditLogs(@PathVariable Long id) {
        return ApiResult.ok(repository.auditLogs(id));
    }
}
