package com.mri.exam.service;

import com.mri.exam.client.PatientClient;
import com.mri.exam.dto.CreateExamOrderRequest;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.repository.ExamOrderRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamOrderServiceTest {
    @Test
    void createExamOrderChecksPatientBeforeSaving() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        CreateExamOrderRequest request = new CreateExamOrderRequest(3L, "头颅MRI平扫", "眩晕待查", "急诊");
        when(patientClient.patientExists(3L)).thenReturn(true);
        when(repository.save(request)).thenReturn(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "REQUESTED", null));

        ExamOrderService service = new ExamOrderService(repository, patientClient);
        ExamOrder order = service.create(request);

        assertThat(order.id()).isEqualTo(11L);
        assertThat(order.status()).isEqualTo("REQUESTED");
        verify(patientClient).patientExists(3L);
    }

    @Test
    void createExamOrderFailsWhenPatientDoesNotExist() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        CreateExamOrderRequest request = new CreateExamOrderRequest(404L, "腰椎MRI", "腰痛", "普通");
        when(patientClient.patientExists(404L)).thenReturn(false);

        ExamOrderService service = new ExamOrderService(repository, patientClient);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("患者不存在");
    }

    @Test
    void completeRequiresExamInProgress() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "REQUESTED", null)));

        ExamOrderService service = new ExamOrderService(repository, patientClient);

        assertThatThrownBy(() -> service.complete(11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IN_PROGRESS");
        verify(repository, never()).updateStatus(11L, "COMPLETED");
    }

    @Test
    void markReportedRequiresCompletedExam() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "REQUESTED", null)));

        ExamOrderService service = new ExamOrderService(repository, patientClient);

        assertThatThrownBy(() -> service.markReported(11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COMPLETED");
        verify(repository, never()).updateStatus(11L, "REPORT_PUBLISHED");
    }

    @Test
    void cancelRejectsCompletedExam() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "COMPLETED", null)));

        ExamOrderService service = new ExamOrderService(repository, patientClient);

        assertThatThrownBy(() -> service.cancel(11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("取消");
        verify(repository, never()).cancel(11L);
    }

    @Test
    void cancelReturnsCancelledExam() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "REQUESTED", null)));
        when(repository.cancel(11L)).thenReturn(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "CANCELLED", null));

        ExamOrderService service = new ExamOrderService(repository, patientClient);
        ExamOrder result = service.cancel(11L);

        assertThat(result.status()).isEqualTo("CANCELLED");
        verify(repository).cancel(11L);
    }

    @Test
    void deleteRemovesExamWhenPresent() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "COMPLETED", null)));

        ExamOrderService service = new ExamOrderService(repository, patientClient);
        service.delete(11L);

        verify(repository).delete(11L);
    }

    @Test
    void deleteFailsWhenExamMissing() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.empty());

        ExamOrderService service = new ExamOrderService(repository, patientClient);

        assertThatThrownBy(() -> service.delete(11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
        verify(repository, never()).delete(11L);
    }
}
