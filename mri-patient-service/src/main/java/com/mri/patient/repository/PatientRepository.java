package com.mri.patient.repository;

import com.mri.common.api.PageResult;
import com.mri.patient.model.Contraindication;
import com.mri.patient.model.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientRepository {
    Patient create(Patient patient);

    Patient createForAccount(Patient patient, String accountUsername);

    Optional<Patient> findById(Long id);

    Optional<Patient> findByAccountUsername(String accountUsername);

    PageResult<Patient> page(long page, long size, String keyword);

    Patient update(Patient patient);

    Patient updateForAccount(Patient patient, String accountUsername);

    void delete(Long id);

    Contraindication createContraindication(Contraindication contraindication);

    Contraindication updateContraindication(Contraindication contraindication);

    Optional<Contraindication> findContraindication(Long id);

    List<Contraindication> listContraindications(Long patientId);

    void deleteContraindication(Long id);

    void replaceContraindications(Long patientId, List<Contraindication> contraindications);
}
