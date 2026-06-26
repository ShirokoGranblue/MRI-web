package com.mri.patient.repository;

import com.mri.patient.entity.PatientEntity;
import com.mri.patient.mapper.ContraindicationMapper;
import com.mri.patient.mapper.PatientMapper;
import com.mri.patient.model.Patient;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MybatisPatientRepositoryTest {
    @Test
    void updateMissingPatientThrows() {
        PatientMapper patientMapper = mock(PatientMapper.class);
        ContraindicationMapper contraindicationMapper = mock(ContraindicationMapper.class);
        when(patientMapper.updateById(any(PatientEntity.class))).thenReturn(0);
        MybatisPatientRepository repository = new MybatisPatientRepository(patientMapper, contraindicationMapper);

        Patient patient = new Patient(99L, "P99", "不存在", "男", LocalDate.of(1988, 1, 1), "13800000000");

        assertThatThrownBy(() -> repository.update(patient))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("患者不存在");
    }

    @Test
    void deleteMissingPatientThrows() {
        PatientMapper patientMapper = mock(PatientMapper.class);
        ContraindicationMapper contraindicationMapper = mock(ContraindicationMapper.class);
        when(patientMapper.deleteById((Serializable) 99L)).thenReturn(0);
        MybatisPatientRepository repository = new MybatisPatientRepository(patientMapper, contraindicationMapper);

        assertThatThrownBy(() -> repository.delete(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("患者不存在");
    }
}
