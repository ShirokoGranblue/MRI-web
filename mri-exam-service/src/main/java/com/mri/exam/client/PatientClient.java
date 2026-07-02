package com.mri.exam.client;

import com.mri.exam.model.PatientContraindication;

import java.util.List;

public interface PatientClient {
    boolean patientExists(Long patientId);

    List<PatientContraindication> contraindications(Long patientId);
}
