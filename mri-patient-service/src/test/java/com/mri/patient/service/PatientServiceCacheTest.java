package com.mri.patient.service;

import com.mri.patient.client.ExamClient;
import com.mri.patient.model.Patient;
import com.mri.patient.repository.PatientRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientServiceCacheTest {
    @Test
    void findPatientCachesFirstDatabaseResult() {
        PatientRepository repository = mock(PatientRepository.class);
        PatientCache cache = new PatientCache();
        Patient patient = new Patient(7L, "P20260618001", "张三", "男", LocalDate.of(1988, 5, 1), "13800000000");
        when(repository.findById(7L)).thenReturn(Optional.of(patient));

        PatientService service = new PatientService(repository, cache, mock(ExamClient.class));

        assertThat(service.findById(7L).name()).isEqualTo("张三");
        assertThat(service.findById(7L).name()).isEqualTo("张三");
        verify(repository, times(1)).findById(7L);
        assertThat(cache.contains(7L)).isTrue();
    }

    @Test
    void updateEvictsCachedPatient() {
        PatientRepository repository = mock(PatientRepository.class);
        PatientCache cache = new PatientCache();
        Patient oldPatient = new Patient(7L, "P20260618001", "张三", "男", LocalDate.of(1988, 5, 1), "13800000000");
        Patient newPatient = new Patient(7L, "P20260618001", "张三丰", "男", LocalDate.of(1988, 5, 1), "13900000000");
        when(repository.findById(7L)).thenReturn(Optional.of(oldPatient), Optional.of(newPatient));
        when(repository.update(newPatient)).thenReturn(newPatient);

        PatientService service = new PatientService(repository, cache, mock(ExamClient.class));
        service.findById(7L);
        service.update(newPatient);

        assertThat(cache.contains(7L)).isFalse();
        assertThat(service.findById(7L).name()).isEqualTo("张三丰");
        verify(repository, times(2)).findById(7L);
    }
}
