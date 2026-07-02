package com.mri.exam.service;

import com.mri.common.exception.ConflictException;
import com.mri.exam.client.PatientClient;
import com.mri.exam.dto.CreateExamOrderRequest;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.model.RiskAssessment;
import com.mri.exam.repository.ExamOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ExamOrderService {
    private final ExamOrderRepository repository;
    private final PatientClient patientClient;
    private final MriRiskService riskService;

    public ExamOrderService(ExamOrderRepository repository, PatientClient patientClient, MriRiskService riskService) {
        this.repository = repository;
        this.patientClient = patientClient;
        this.riskService = riskService;
    }

    @Transactional
    public ExamOrder create(CreateExamOrderRequest request) {
        if (!patientClient.patientExists(request.patientId())) {
            throw new IllegalArgumentException("患者不存在，不能创建 MRI 检查申请");
        }
        return repository.save(request, evaluate(request.patientId()));
    }

    public ExamOrder createForPatient(Long patientId, CreateExamOrderRequest request) {
        return create(new CreateExamOrderRequest(
                patientId,
                request.examItem(),
                request.clinicalDiagnosis(),
                request.priority()
        ));
    }

    @Transactional
    public ExamOrder update(Long id, CreateExamOrderRequest request) {
        repository.findById(id).orElseThrow(() -> new IllegalArgumentException("检查申请不存在"));
        if (!patientClient.patientExists(request.patientId())) {
            throw new IllegalArgumentException("患者不存在，不能修改 MRI 检查申请");
        }
        return repository.update(id, request, evaluate(request.patientId()));
    }

    public RiskAssessment risk(Long id) {
        ExamOrder order = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("检查申请不存在"));
        return evaluate(order.patientId());
    }

    @Transactional(noRollbackFor = ConflictException.class)
    public ExamOrder start(Long id, boolean confirmHighRisk, String operator) {
        ExamOrder order = requireStatus(id, "REQUESTED");
        RiskAssessment risk = evaluate(order.patientId());
        if ("HIGH".equals(risk.level())) {
            if (!confirmHighRisk) {
                repository.updateRisk(id, risk, null, null);
                throw new ConflictException("当前检查存在高风险 MRI 禁忌症，请完成风险评估并明确确认后再开始检查");
            }
            if (operator == null || operator.isBlank()) {
                throw new IllegalArgumentException("无法确认当前医生身份");
            }
            repository.updateRisk(id, risk, operator.trim(), LocalDateTime.now());
        } else {
            repository.updateRisk(id, risk, null, null);
        }
        return repository.updateStatus(id, "IN_PROGRESS");
    }

    public ExamOrder start(Long id) {
        return start(id, false, null);
    }

    public ExamOrder complete(Long id) {
        requireStatus(id, "IN_PROGRESS");
        return repository.updateStatus(id, "COMPLETED");
    }

    public ExamOrder markReported(Long id) {
        requireStatus(id, "COMPLETED");
        return repository.updateStatus(id, "REPORT_PUBLISHED");
    }

    public ExamOrder cancel(Long id) {
        ExamOrder order = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("检查申请不存在"));
        if (!java.util.Set.of("REQUESTED", "IN_PROGRESS").contains(order.status())) {
            throw new IllegalArgumentException("仅待检查或检查中的申请可取消，当前状态为“" + statusLabel(order.status()) + "”");
        }
        return repository.cancel(id);
    }

    public void delete(Long id) {
        repository.findById(id).orElseThrow(() -> new IllegalArgumentException("检查申请不存在"));
        repository.delete(id);
    }

    public java.util.List<ExamOrder> listByPatient(Long patientId) {
        return repository.listByPatient(patientId);
    }

    private RiskAssessment evaluate(Long patientId) {
        return riskService.assess(patientClient.contraindications(patientId));
    }

    private ExamOrder requireStatus(Long id, String expectedStatus) {
        ExamOrder order = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("检查申请不存在"));
        if (!expectedStatus.equals(order.status())) {
            throw new IllegalArgumentException("检查申请必须处于“" + statusLabel(expectedStatus)
                    + "”，当前状态为“" + statusLabel(order.status()) + "”");
        }
        return order;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "REQUESTED" -> "待检查";
            case "IN_PROGRESS" -> "检查中";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "REPORT_PUBLISHED" -> "已出报告";
            default -> "未知状态";
        };
    }
}
