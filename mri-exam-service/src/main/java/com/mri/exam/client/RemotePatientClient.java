package com.mri.exam.client;

import org.springframework.stereotype.Component;

@Component
public class RemotePatientClient implements PatientClient {
    private final PatientFeignApi api;

    public RemotePatientClient(PatientFeignApi api) {
        this.api = api;
    }

    @Override
    public boolean patientExists(Long patientId) {
        try {
            return Boolean.TRUE.equals(api.exists(patientId).data().get("exists"));
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
