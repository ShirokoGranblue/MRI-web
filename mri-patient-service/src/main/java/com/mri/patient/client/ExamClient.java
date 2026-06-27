package com.mri.patient.client;

import java.util.List;

public interface ExamClient {
    List<ExamSummary> listByPatient(Long patientId);
}
