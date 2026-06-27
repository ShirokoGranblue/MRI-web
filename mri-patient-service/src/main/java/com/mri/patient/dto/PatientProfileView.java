package com.mri.patient.dto;

import com.mri.patient.model.Contraindication;
import com.mri.patient.model.Patient;

import java.util.List;

public record PatientProfileView(
        boolean profileComplete,
        Patient patient,
        boolean hasContraindications,
        List<Contraindication> contraindications
) {
    public static PatientProfileView incomplete() {
        return new PatientProfileView(false, null, false, List.of());
    }
}
