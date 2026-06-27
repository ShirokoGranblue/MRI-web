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
        String examStatus = examClient.examStatus(request.examOrderId());
        if (!"COMPLETED".equals(examStatus)) {
            throw new IllegalArgumentException("检查尚未完成，不能创建报告");
        }
        if (!imageClient.studyExists(request.studyId())) {
            throw new IllegalArgumentException("对应影像未归档，不能创建报告");
        }
        return repository.createDraft(request);
    }

    public Report submit(Long id) {
        Report report = requireReport(id);
        requireStatus(report, "DRAFT");
        Report submitted = repository.updateStatus(id, "SUBMITTED");
        repository.audit(id, "SUBMIT", "diagnosis-doctor", "提交审核");
        return submitted;
    }

    public Report approve(Long id) {
        Report report = requireReport(id);
        requireStatus(report, "SUBMITTED");
        Report approved = repository.updateStatus(id, "APPROVED");
        repository.audit(id, "APPROVE", "audit-doctor", "审核通过");
        return approved;
    }

    public Report reject(Long id, String reason) {
        Report report = requireReport(id);
        requireStatus(report, "SUBMITTED");
        Report rejected = repository.updateStatus(id, "REJECTED");
        repository.audit(id, "REJECT", "audit-doctor", reason);
        return rejected;
    }

    public Report reopen(Long id) {
        Report report = requireReport(id);
        requireStatus(report, "REJECTED");
        Report draft = repository.updateStatus(id, "DRAFT");
        repository.audit(id, "REOPEN", "diagnosis-doctor", "回到草稿修改");
        return draft;
    }

    public Report publish(Long id) {
        Report report = requireReport(id);
        requireStatus(report, "APPROVED");
        String studyDescription = imageClient.studyDescription(report.studyId());
        examClient.markReported(report.examOrderId());
        Report published = repository.updateStatus(id, "PUBLISHED");
        repository.audit(id, "PUBLISH", "audit-doctor", "发布报告，Study=" + studyDescription);
        return published;
    }

    private Report requireReport(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("报告不存在"));
    }

    private static void requireStatus(Report report, String expectedStatus) {
        if (!expectedStatus.equals(report.status())) {
            throw new IllegalArgumentException("报告状态必须为 " + expectedStatus + "，当前状态为 " + report.status());
        }
    }
}
