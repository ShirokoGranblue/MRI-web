package com.mri.exam.service;

import com.mri.exam.client.PatientClient;
import com.mri.exam.dto.CreateExamOrderRequest;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.repository.ExamOrderRepository;
import org.springframework.stereotype.Service;

@Service
public class ExamOrderService {
    private final ExamOrderRepository repository;
    private final PatientClient patientClient;

    public ExamOrderService(ExamOrderRepository repository, PatientClient patientClient) {
        this.repository = repository;
        this.patientClient = patientClient;
    }

    public ExamOrder create(CreateExamOrderRequest request) {
        if (!patientClient.patientExists(request.patientId())) {
            throw new IllegalArgumentException("患者不存在，不能创建 MRI 检查申请");
        }
        return repository.save(request);
    }

    public ExamOrder start(Long id) {
        requireStatus(id, "REQUESTED");
        return repository.updateStatus(id, "IN_PROGRESS");
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
            throw new IllegalArgumentException("仅待检查或进行中的检查可取消，当前状态为 " + order.status());
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

    private void requireStatus(Long id, String expectedStatus) {
        ExamOrder order = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("检查申请不存在"));
        if (!expectedStatus.equals(order.status())) {
            throw new IllegalArgumentException("检查申请状态必须为 " + expectedStatus + "，当前状态为 " + order.status());
        }
    }
}
