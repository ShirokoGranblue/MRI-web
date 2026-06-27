package com.mri.exam.service;

import com.mri.exam.mapper.PatientExamAccessMapper;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.model.MriSchedule;
import com.mri.exam.repository.ExamOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientExamQueryServiceTest {
    @Test
    void returnsOnlyExamsResolvedFromCurrentUsernameWithSchedules() {
        PatientExamAccessMapper access = mock(PatientExamAccessMapper.class);
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        ExamOrder exam = new ExamOrder(21L, 12L, "头颅 MRI", "头痛", "NORMAL", "IN_PROGRESS", LocalDateTime.now());
        MriSchedule schedule = new MriSchedule(3L, 21L, "MRI-01", LocalDateTime.now().plusDays(1), "李医生");
        when(access.findPatientId("patient01")).thenReturn(12L);
        when(repository.listByPatient(12L)).thenReturn(List.of(exam));
        when(repository.listSchedules(21L)).thenReturn(List.of(schedule));

        PatientExamQueryService service = new PatientExamQueryService(access, repository);

        assertThat(service.findMine("patient01")).singleElement()
                .satisfies(view -> {
                    assertThat(view.id()).isEqualTo(21L);
                    assertThat(view.schedules()).containsExactly(schedule);
                });
    }
}
