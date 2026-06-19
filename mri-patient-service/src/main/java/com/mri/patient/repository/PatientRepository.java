package com.mri.patient.repository;

import com.mri.common.api.PageResult;
import com.mri.patient.model.Contraindication;
import com.mri.patient.model.Patient;
import com.mri.patient.model.PatientExamHistory;

import java.util.List;
import java.util.Optional;

public interface PatientRepository {
    Patient create(Patient patient);

    Optional<Patient> findById(Long id);

    PageResult<Patient> page(long page, long size, String keyword);

    Patient update(Patient patient);

    void delete(Long id);

    Contraindication createContraindication(Contraindication contraindication);

    Contraindication updateContraindication(Contraindication contraindication);

    Optional<Contraindication> findContraindication(Long id);

    List<Contraindication> listContraindications(Long patientId);

    void deleteContraindication(Long id);

    List<PatientExamHistory> examHistory(Long patientId);
}
