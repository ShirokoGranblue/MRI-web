package com.mri.patient.dto;

import java.time.LocalDate;
import java.util.List;

public record PatientProfileRequest(
        String name,
        String gender,
        LocalDate birthDate,
        String phone,
        boolean hasContraindications,
        List<ContraindicationInput> contraindications
) {
    public record ContraindicationInput(String type, String description, String severity) {
    }
}
