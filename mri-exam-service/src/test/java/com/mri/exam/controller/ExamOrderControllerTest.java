package com.mri.exam.controller;

import com.mri.exam.dto.CreateExamOrderRequest;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.repository.ExamOrderRepository;
import com.mri.exam.service.ExamOrderService;
import com.mri.exam.service.PatientExamQueryService;
import com.mri.exam.service.ScheduleService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamOrderControllerTest {
    @Test
    void patientCreateUsesPatientResolvedFromAuthenticatedUsername() {
        ExamOrderService service = mock(ExamOrderService.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        PatientExamQueryService patientQuery = mock(PatientExamQueryService.class);
        CreateExamOrderRequest request = new CreateExamOrderRequest(999L, "头颅MRI平扫", "头痛待查", "普通");
        ExamOrder created = new ExamOrder(11L, 12L, "头颅MRI平扫", "头痛待查", "普通", "REQUESTED", null);
        when(patientQuery.requirePatientId("patient01")).thenReturn(12L);
        when(service.createForPatient(12L, request)).thenReturn(created);

        ExamOrderController controller = new ExamOrderController(
                service,
                repository,
                patientQuery,
                mock(ScheduleService.class)
        );
        ExamOrder result = controller.create(request, "patient01", "PATIENT").data();

        assertThat(result.patientId()).isEqualTo(12L);
        verify(service).createForPatient(12L, request);
    }
}
