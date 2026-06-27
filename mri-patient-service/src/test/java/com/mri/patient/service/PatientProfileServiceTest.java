package com.mri.patient.service;

import com.mri.common.exception.ConflictException;
import com.mri.patient.client.ExamClient;
import com.mri.patient.dto.PatientProfileRequest;
import com.mri.patient.dto.PatientProfileView;
import com.mri.patient.model.Contraindication;
import com.mri.patient.model.Patient;
import com.mri.patient.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientProfileServiceTest {
    @Test
    void profileIsIncompleteBeforePatientCreatesIt() {
        PatientRepository repository = mock(PatientRepository.class);
        when(repository.findByAccountUsername("patient01")).thenReturn(Optional.empty());
        PatientService service = service(repository);

        PatientProfileView view = service.profileFor("patient01");

        assertThat(view.profileComplete()).isFalse();
        assertThat(view.patient()).isNull();
        assertThat(view.contraindications()).isEmpty();
    }

    @Test
    void createProfileGeneratesPatientNumberAndSavesContraindications() {
        PatientRepository repository = mock(PatientRepository.class);
        Patient requestPatient = new Patient(null, null, "张三", "男", LocalDate.of(1990, 1, 1), "13800000000");
        Patient created = new Patient(12L, "P20260627A1B2C3D4", "张三", "男", LocalDate.of(1990, 1, 1), "13800000000");
        when(repository.findByAccountUsername("patient01"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(created));
        when(repository.createForAccount(any(Patient.class), eq("patient01"))).thenReturn(created);
        when(repository.listContraindications(12L)).thenReturn(List.of(
                new Contraindication(31L, 12L, "金属植入物", "左膝钛合金内固定", "HIGH")
        ));
        PatientService service = service(repository);

        PatientProfileView view = service.createProfile("patient01", new PatientProfileRequest(
                requestPatient.name(),
                requestPatient.gender(),
                requestPatient.birthDate(),
                requestPatient.phone(),
                true,
                List.of(new PatientProfileRequest.ContraindicationInput("金属植入物", "左膝钛合金内固定", "HIGH"))
        ));

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(repository).createForAccount(patientCaptor.capture(), eq("patient01"));
        assertThat(patientCaptor.getValue().patientNo()).matches("P\\d{8}[A-F0-9]{8}");
        verify(repository).replaceContraindications(eq(12L), any());
        assertThat(view.profileComplete()).isTrue();
        assertThat(view.hasContraindications()).isTrue();
        assertThat(view.contraindications()).hasSize(1);
    }

    @Test
    void createProfileRejectsSecondProfileForSameAccount() {
        PatientRepository repository = mock(PatientRepository.class);
        when(repository.findByAccountUsername("patient01")).thenReturn(Optional.of(
                new Patient(12L, "P1", "张三", "男", LocalDate.of(1990, 1, 1), null)
        ));
        PatientService service = service(repository);

        assertThatThrownBy(() -> service.createProfile("patient01", request(false, List.of())))
                .isInstanceOf(ConflictException.class)
                .hasMessage("患者档案已存在，请使用修改功能");
    }

    @Test
    void contraindicationFlagMustMatchSubmittedList() {
        PatientService service = service(mock(PatientRepository.class));

        assertThatThrownBy(() -> service.createProfile("patient01", request(true, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("请至少登记一条禁忌症");
        assertThatThrownBy(() -> service.createProfile("patient01", request(false, List.of(
                new PatientProfileRequest.ContraindicationInput("妊娠", "", "HIGH")
        ))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("选择无禁忌症时不能提交禁忌症记录");
    }

    private static PatientProfileRequest request(boolean hasContraindications,
                                                 List<PatientProfileRequest.ContraindicationInput> contraindications) {
        return new PatientProfileRequest("张三", "男", LocalDate.of(1990, 1, 1), "13800000000",
                hasContraindications, contraindications);
    }

    private static PatientService service(PatientRepository repository) {
        return new PatientService(repository, mock(PatientCache.class), mock(ExamClient.class));
    }
}
