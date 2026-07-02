package com.mri.report.service;

import com.mri.report.client.ExamClient;
import com.mri.report.client.ImageClient;
import com.mri.report.dto.CreateReportRequest;
import com.mri.report.model.Report;
import com.mri.report.repository.ReportRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final ReportRepository repository;
    private final ImageClient imageClient;
    private final ExamClient examClient;

    public ReportService(ReportRepository repository, ImageClient imageClient, ExamClient examClient) {
        this.repository = repository;
        this.imageClient = imageClient;
        this.examClient = examClient;
    }

    public Report create(CreateReportRequest request) {
        validateRequest(request);
        return repository.createDraft(request);
    }

    private void validateRequest(CreateReportRequest request) {
        String examStatus = examClient.examStatus(request.examOrderId());
        if (!"COMPLETED".equals(examStatus)) {
            throw new IllegalArgumentException("检查尚未完成，不能创建报告");
        }
        Long studyExamOrderId = imageClient.studyExamOrderId(request.studyId());
        if (studyExamOrderId == null) {
            throw new IllegalArgumentException("对应影像未归档，不能创建报告");
        }
        if (!request.examOrderId().equals(studyExamOrderId)) {
            throw new IllegalArgumentException("所选影像不属于该检查申请");
        }
    }

    public Report submit(Long id, String operator) {
        operator = requireOperator(operator);
        Report report = requireReport(id);
        requireStatus(report, "DRAFT");
        Report submitted = repository.updateStatus(id, "SUBMITTED");
        repository.audit(id, "SUBMIT", operator, "提交审核");
        return submitted;
    }

    public Report approve(Long id, String operator) {
        operator = requireOperator(operator);
        Report report = requireReport(id);
        requireStatus(report, "SUBMITTED");
        Report approved = repository.updateStatus(id, "APPROVED");
        repository.audit(id, "APPROVE", operator, "审核通过");
        return approved;
    }

    public Report reject(Long id, String operator, String reason) {
        operator = requireOperator(operator);
        Report report = requireReport(id);
        requireStatus(report, "SUBMITTED");
        Report rejected = repository.updateStatus(id, "REJECTED");
        repository.audit(id, "REJECT", operator, reason);
        return rejected;
    }

    public Report reopen(Long id, String operator) {
        operator = requireOperator(operator);
        Report report = requireReport(id);
        requireStatus(report, "REJECTED");
        Report draft = repository.updateStatus(id, "DRAFT");
        repository.audit(id, "REOPEN", operator, "回到草稿修改");
        return draft;
    }

    public Report publish(Long id, String operator) {
        operator = requireOperator(operator);
        Report report = requireReport(id);
        requireStatus(report, "APPROVED");
        String studyDescription = imageClient.studyDescription(report.studyId());
        examClient.markReported(report.examOrderId());
        Report published = repository.updateStatus(id, "PUBLISHED");
        repository.audit(id, "PUBLISH", operator, "发布报告，Study=" + studyDescription);
        return published;
    }

    public Report update(Long id, CreateReportRequest request) {
        Report report = requireReport(id);
        requireEditable(report, "修改");
        validateRequest(request);
        return repository.update(id, request);
    }

    public void delete(Long id) {
        Report report = requireReport(id);
        requireEditable(report, "删除");
        repository.delete(id);
    }

    private Report requireReport(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("报告不存在"));
    }

    private static String requireOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            throw new IllegalArgumentException("无法确认当前登录用户");
        }
        return operator.trim();
    }

    private static void requireStatus(Report report, String expectedStatus) {
        if (!expectedStatus.equals(report.status())) {
            throw new IllegalArgumentException("报告必须处于“" + statusLabel(expectedStatus)
                    + "”，当前状态为“" + statusLabel(report.status()) + "”");
        }
    }

    private static void requireEditable(Report report, String action) {
        if ("PUBLISHED".equals(report.status())) {
            throw new IllegalArgumentException("已发布报告不能" + action);
        }
        if (!"DRAFT".equals(report.status()) && !"REJECTED".equals(report.status())) {
            throw new IllegalArgumentException("仅草稿或已驳回报告可以" + action);
        }
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "SUBMITTED" -> "待审核";
            case "APPROVED" -> "已审核";
            case "REJECTED" -> "已驳回";
            case "PUBLISHED" -> "已发布";
            default -> "未知状态";
        };
    }
}
