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
        return repository.updateStatus(id, "IN_PROGRESS");
    }

    public ExamOrder complete(Long id) {
        return repository.updateStatus(id, "COMPLETED");
    }

    public ExamOrder markReported(Long id) {
        return repository.updateStatus(id, "REPORT_PUBLISHED");
    }
}
