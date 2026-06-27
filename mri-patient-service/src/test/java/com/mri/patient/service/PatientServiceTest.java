package com.mri.patient.service;

import com.mri.patient.client.ExamClient;
import com.mri.patient.client.ExamSummary;
import com.mri.patient.model.PatientExamHistory;
import com.mri.patient.repository.PatientRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientServiceTest {
    @Test
    void examHistoryMapsRemoteExams() {
        PatientRepository repository = mock(PatientRepository.class);
        PatientCache cache = mock(PatientCache.class);
        ExamClient examClient = mock(ExamClient.class);
        when(examClient.listByPatient(3L)).thenReturn(List.of(
                new ExamSummary(3L, "头颅MRI平扫", "COMPLETED", LocalDateTime.now().minusDays(1))));

        PatientService service = new PatientService(repository, cache, examClient);

        List<PatientExamHistory> history = service.examHistory(3L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).examItem()).isEqualTo("头颅MRI平扫");
        assertThat(history.get(0).status()).isEqualTo("COMPLETED");
    }

    @Test
    void examHistoryReturnsEmptyWhenRemoteFails() {
        PatientRepository repository = mock(PatientRepository.class);
        PatientCache cache = mock(PatientCache.class);
        ExamClient examClient = mock(ExamClient.class);
        when(examClient.listByPatient(3L)).thenReturn(List.of());

        PatientService service = new PatientService(repository, cache, examClient);

        assertThat(service.examHistory(3L)).isEmpty();
    }
}
