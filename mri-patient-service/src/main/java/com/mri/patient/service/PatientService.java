package com.mri.patient.service;

import com.mri.common.api.PageResult;
import com.mri.patient.model.Patient;
import com.mri.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class PatientService {
    private final PatientRepository repository;
    private final PatientCache cache;

    public PatientService(PatientRepository repository, PatientCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    public Patient create(Patient patient) {
        Patient created = repository.create(patient);
        cache.evict(created.id());
        return created;
    }

    public Patient findById(Long id) {
        return cache.get(id).orElseGet(() -> {
            Patient patient = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("患者不存在"));
            cache.put(patient);
            return patient;
        });
    }

    public PageResult<Patient> page(long page, long size, String keyword) {
        return repository.page(page, size, keyword);
    }

    public Patient update(Patient patient) {
        Patient updated = repository.update(patient);
        cache.evict(patient.id());
        return updated;
    }

    public void delete(Long id) {
        repository.delete(id);
        cache.evict(id);
    }
}
