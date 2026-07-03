package com.mri.exam.service;

import com.mri.common.exception.ConflictException;
import com.mri.exam.client.PatientClient;
import com.mri.exam.dto.CreateExamOrderRequest;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.model.PatientContraindication;
import com.mri.exam.model.RiskAssessment;
import com.mri.exam.repository.ExamOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamOrderServiceTest {
    @Test
    void patientExamRequestUsesAuthenticatedPatientIdInsteadOfRequestBodyId() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        CreateExamOrderRequest request = new CreateExamOrderRequest(999L, "头颅MRI平扫", "头痛待查", "普通");
        when(patientClient.patientExists(12L)).thenReturn(true);
        when(repository.save(eq(new CreateExamOrderRequest(12L, "头颅MRI平扫", "头痛待查", "普通")), any(RiskAssessment.class)))
                .thenReturn(new ExamOrder(11L, 12L, "头颅MRI平扫", "头痛待查", "普通", "REQUESTED", null));

        ExamOrderService service = newService(repository, patientClient);
        ExamOrder order = service.createForPatient(12L, request);

        assertThat(order.patientId()).isEqualTo(12L);
        verify(patientClient).patientExists(12L);
        verify(patientClient, never()).patientExists(999L);
        verify(repository).save(eq(new CreateExamOrderRequest(12L, "头颅MRI平扫", "头痛待查", "普通")), any(RiskAssessment.class));
    }

    @Test
    void createExamOrderChecksPatientBeforeSaving() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        CreateExamOrderRequest request = new CreateExamOrderRequest(3L, "头颅MRI平扫", "眩晕待查", "急诊");
        when(patientClient.patientExists(3L)).thenReturn(true);
        when(repository.save(eq(request), any(RiskAssessment.class))).thenReturn(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "REQUESTED", null));

        ExamOrderService service = newService(repository, patientClient);
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

        ExamOrderService service = newService(repository, patientClient);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("患者不存在");
    }

    @Test
    void completeRequiresExamInProgress() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "REQUESTED", null)));

        ExamOrderService service = newService(repository, patientClient);

        assertThatThrownBy(() -> service.complete(11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("检查中");
        verify(repository, never()).updateStatus(11L, "COMPLETED");
    }

    @Test
    void markReportedRequiresCompletedExam() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "REQUESTED", null)));

        ExamOrderService service = newService(repository, patientClient);

        assertThatThrownBy(() -> service.markReported(11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已完成");
        verify(repository, never()).updateStatus(11L, "REPORT_PUBLISHED");
    }

    @Test
    void cancelRejectsCompletedExam() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "COMPLETED", null)));

        ExamOrderService service = newService(repository, patientClient);

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

        ExamOrderService service = newService(repository, patientClient);
        ExamOrder result = service.cancel(11L);

        assertThat(result.status()).isEqualTo("CANCELLED");
        verify(repository).cancel(11L);
    }

    @Test
    void deleteRemovesExamWhenPresent() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕待查", "急诊", "COMPLETED", null)));

        ExamOrderService service = newService(repository, patientClient);
        service.delete(11L);

        verify(repository).delete(11L);
    }

    @Test
    void deleteFailsWhenExamMissing() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.empty());

        ExamOrderService service = newService(repository, patientClient);

        assertThatThrownBy(() -> service.delete(11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
        verify(repository, never()).delete(11L);
    }

    @Test
    void createStoresCurrentRiskSnapshot() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        CreateExamOrderRequest request = new CreateExamOrderRequest(3L, "头颅MRI平扫", "眩晕", "普通");
        when(patientClient.patientExists(3L)).thenReturn(true);
        when(patientClient.contraindications(3L)).thenReturn(List.of(
                new PatientContraindication(8L, 3L, "心脏起搏器", "型号待核实", "HIGH")
        ));
        when(repository.save(eq(request), any(RiskAssessment.class))).thenAnswer(invocation -> {
            RiskAssessment risk = invocation.getArgument(1);
            return new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕", "普通", "REQUESTED", null,
                    risk.level(), risk.summary(), risk.evaluatedAt(), null, null);
        });

        ExamOrder order = new ExamOrderService(repository, patientClient, new MriRiskService()).create(request);

        assertThat(order.riskLevel()).isEqualTo("HIGH");
        assertThat(order.riskSummary()).contains("心脏起搏器");
    }

    @Test
    void highRiskStartRequiresExplicitConfirmationAndKeepsStatusRequested() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(
                new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕", "普通", "REQUESTED", null)));
        when(patientClient.contraindications(3L)).thenReturn(List.of(
                new PatientContraindication(8L, 3L, "心脏起搏器", "新增禁忌症", "HIGH")
        ));

        ExamOrderService service = new ExamOrderService(repository, patientClient, new MriRiskService());

        assertThatThrownBy(() -> service.start(11L, false, "doctor"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("高风险");
        verify(repository).updateRisk(eq(11L), any(RiskAssessment.class), eq(null), eq(null));
        verify(repository, never()).updateStatus(11L, "IN_PROGRESS");
    }

    @Test
    void confirmedHighRiskStartRecordsTrustedDoctorAndTime() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(
                new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕", "普通", "REQUESTED", null)));
        when(patientClient.contraindications(3L)).thenReturn(List.of(
                new PatientContraindication(8L, 3L, "心脏起搏器", "已完成专科评估", "HIGH")
        ));
        when(repository.updateStatus(11L, "IN_PROGRESS")).thenReturn(
                new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕", "普通", "IN_PROGRESS", null));

        ExamOrder result = new ExamOrderService(repository, patientClient, new MriRiskService())
                .start(11L, true, "doctor");

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        verify(repository).updateRisk(eq(11L), any(RiskAssessment.class), eq("doctor"), any(LocalDateTime.class));
    }

    @Test
    void patientServiceFailurePreventsExamStart() {
        PatientClient patientClient = mock(PatientClient.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(
                new ExamOrder(11L, 3L, "头颅MRI平扫", "眩晕", "普通", "REQUESTED", null)));
        when(patientClient.contraindications(3L)).thenThrow(new IllegalStateException("患者禁忌症服务暂时不可用"));

        ExamOrderService service = new ExamOrderService(repository, patientClient, new MriRiskService());

        assertThatThrownBy(() -> service.start(11L, false, "doctor"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("暂时不可用");
        verify(repository, never()).updateStatus(11L, "IN_PROGRESS");
    }

    private static ExamOrderService newService(ExamOrderRepository repository, PatientClient patientClient) {
        return new ExamOrderService(repository, patientClient, new MriRiskService());
    }
}
