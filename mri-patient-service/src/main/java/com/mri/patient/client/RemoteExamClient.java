package com.mri.patient.client;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RemoteExamClient implements ExamClient {
    private final ExamFeignApi api;

    public RemoteExamClient(ExamFeignApi api) {
        this.api = api;
    }

    @Override
    public List<ExamSummary> listByPatient(Long patientId) {
        try {
            return api.byPatient(patientId).data();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }
}
