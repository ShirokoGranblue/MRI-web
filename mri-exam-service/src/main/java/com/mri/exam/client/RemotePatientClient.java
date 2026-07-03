package com.mri.exam.client;

import com.mri.exam.model.PatientContraindication;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public List<PatientContraindication> contraindications(Long patientId) {
        try {
            List<PatientContraindication> result = api.contraindications(patientId).data();
            return result == null ? List.of() : result;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("患者禁忌症服务暂时不可用，不能评估 MRI 检查风险", ex);
        }
    }
}
