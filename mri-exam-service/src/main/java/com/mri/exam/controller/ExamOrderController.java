package com.mri.exam.controller;

import com.mri.common.api.ApiResult;
import com.mri.common.api.PageResult;
import com.mri.exam.dto.CreateExamOrderRequest;
import com.mri.exam.dto.PatientExamView;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.model.MriSchedule;
import com.mri.exam.repository.ExamOrderRepository;
import com.mri.exam.service.ExamOrderService;
import com.mri.exam.service.PatientExamQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "MRI 检查接口", description = "检查申请、排程、状态流转")
@RestController
@RequestMapping("/exams")
public class ExamOrderController {
    private final ExamOrderService service;
    private final ExamOrderRepository repository;
    private final PatientExamQueryService patientQuery;

    public ExamOrderController(ExamOrderService service, ExamOrderRepository repository, PatientExamQueryService patientQuery) {
        this.service = service;
        this.repository = repository;
        this.patientQuery = patientQuery;
    }

    @Operation(summary = "当前患者本人检查与排程")
    @GetMapping("/mine")
    public ApiResult<List<PatientExamView>> mine(@RequestHeader("X-Authenticated-User") String username) {
        return ApiResult.ok(patientQuery.findMine(username));
    }

    @Operation(summary = "新增 MRI 检查申请")
    @PostMapping
    public ApiResult<ExamOrder> create(@RequestBody CreateExamOrderRequest request) {
        return ApiResult.ok(service.create(request));
    }

    @Operation(summary = "取消 MRI 检查申请")
    @PostMapping("/{id}/cancel")
    public ApiResult<ExamOrder> cancel(@PathVariable Long id) {
        return ApiResult.ok(service.cancel(id));
    }

    @Operation(summary = "删除 MRI 检查申请")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok();
    }

    @Operation(summary = "修改 MRI 检查申请")
    @PutMapping("/{id}")
    public ApiResult<ExamOrder> update(@PathVariable Long id, @RequestBody CreateExamOrderRequest request) {
        return ApiResult.ok(repository.update(id, request));
    }

    @Operation(summary = "MRI 检查申请详情")
    @GetMapping("/{id}")
    public ApiResult<ExamOrder> detail(@PathVariable Long id) {
        return ApiResult.ok(repository.findById(id).orElseThrow(() -> new IllegalArgumentException("检查申请不存在")));
    }

    @Operation(summary = "MRI 检查申请分页查询")
    @GetMapping
    public ApiResult<PageResult<ExamOrder>> page(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String status) {
        return ApiResult.ok(repository.page(page, size, status));
    }

    @Operation(summary = "按患者查询检查申请")
    @GetMapping("/by-patient/{patientId}")
    public ApiResult<List<ExamOrder>> byPatient(@PathVariable Long patientId) {
        return ApiResult.ok(service.listByPatient(patientId));
    }

    @Operation(summary = "新增检查排程")
    @PostMapping("/schedules")
    public ApiResult<MriSchedule> createSchedule(@RequestBody MriSchedule schedule) {
        return ApiResult.ok(repository.createSchedule(schedule));
    }

    @Operation(summary = "修改检查排程")
    @PutMapping("/schedules/{id}")
    public ApiResult<MriSchedule> updateSchedule(@PathVariable Long id, @RequestBody MriSchedule schedule) {
        return ApiResult.ok(repository.updateSchedule(new MriSchedule(id, schedule.examOrderId(), schedule.scannerRoom(), schedule.scheduledAt(), schedule.technologist())));
    }

    @Operation(summary = "删除检查排程")
    @DeleteMapping("/schedules/{id}")
    public ApiResult<Void> deleteSchedule(@PathVariable Long id) {
        repository.deleteSchedule(id);
        return ApiResult.ok();
    }

    @Operation(summary = "检查排程详情")
    @GetMapping("/schedules/{id}")
    public ApiResult<MriSchedule> scheduleDetail(@PathVariable Long id) {
        return ApiResult.ok(repository.findSchedule(id).orElseThrow(() -> new IllegalArgumentException("检查排程不存在")));
    }

    @Operation(summary = "检查排程列表")
    @GetMapping("/{examOrderId}/schedules")
    public ApiResult<List<MriSchedule>> listSchedules(@PathVariable Long examOrderId) {
        return ApiResult.ok(repository.listSchedules(examOrderId));
    }

    @Operation(summary = "开始 MRI 检查")
    @PostMapping("/{id}/start")
    public ApiResult<ExamOrder> start(@PathVariable Long id) {
        return ApiResult.ok(service.start(id));
    }

    @Operation(summary = "完成 MRI 检查")
    @PostMapping("/{id}/complete")
    public ApiResult<ExamOrder> complete(@PathVariable Long id) {
        return ApiResult.ok(service.complete(id));
    }

    @Operation(summary = "检查状态查询")
    @GetMapping("/{id}/status")
    public ApiResult<Map<String, String>> status(@PathVariable Long id) {
        return ApiResult.ok(Map.of("status", repository.findById(id).orElseThrow(() -> new IllegalArgumentException("检查申请不存在")).status()));
    }

    @Operation(summary = "检查申请存在性校验")
    @GetMapping("/{id}/exists")
    public ApiResult<Map<String, Boolean>> exists(@PathVariable Long id) {
        return ApiResult.ok(Map.of("exists", repository.findById(id).isPresent()));
    }

    @Operation(summary = "标记检查已有报告")
    @PostMapping("/{id}/reported")
    public ApiResult<ExamOrder> markReported(@PathVariable Long id) {
        return ApiResult.ok(service.markReported(id));
    }
}
