package com.mri.exam.service;

import com.mri.common.exception.ConflictException;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.model.MriSchedule;
import com.mri.exam.repository.ExamOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 7, 1, 10, 0);

    @Test
    void sameRoomOverlapIsRejectedWithBusinessMessage() {
        ExamOrderRepository repository = requestedRepository();
        when(repository.listSchedulesForConflict()).thenReturn(List.of(
                new MriSchedule(1L, 10L, "MRI-01", TEN, "张某", 30)
        ));

        ScheduleService service = new ScheduleService(repository);
        MriSchedule incoming = new MriSchedule(null, 11L, " MRI-01 ", TEN.plusMinutes(15), "李某", 30);

        assertThatThrownBy(() -> service.create(incoming))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("MRI-01")
                .hasMessageContaining("10:00—10:30")
                .hasMessageContaining("检查申请 10");
        verify(repository, never()).createSchedule(incoming);
    }

    @Test
    void sameTechnologistOverlapAcrossRoomsIsRejected() {
        ExamOrderRepository repository = requestedRepository();
        when(repository.listSchedulesForConflict()).thenReturn(List.of(
                new MriSchedule(1L, 10L, "MRI-01", TEN, "张某", 45)
        ));

        assertThatThrownBy(() -> new ScheduleService(repository).create(
                new MriSchedule(null, 11L, "MRI-02", TEN.plusMinutes(30), " 张某 ", 30)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("技师 张某")
                .hasMessageContaining("10:00—10:45");
    }

    @Test
    void backToBackSchedulesAreAllowed() {
        ExamOrderRepository repository = requestedRepository();
        when(repository.listSchedulesForConflict()).thenReturn(List.of(
                new MriSchedule(1L, 10L, "MRI-01", TEN, "张某", 30)
        ));
        MriSchedule incoming = new MriSchedule(null, 11L, "MRI-01", TEN.plusMinutes(30), "张某", 30);
        when(repository.createSchedule(incoming)).thenReturn(
                new MriSchedule(2L, 11L, "MRI-01", TEN.plusMinutes(30), "张某", 30));

        MriSchedule result = new ScheduleService(repository).create(incoming);

        assertThat(result.id()).isEqualTo(2L);
    }

    @Test
    void blankTechnologistOnlyChecksRoomConflictsAndValuesAreTrimmed() {
        ExamOrderRepository repository = requestedRepository();
        when(repository.listSchedulesForConflict()).thenReturn(List.of(
                new MriSchedule(1L, 10L, "MRI-01", TEN, "张某", 30)
        ));
        MriSchedule expected = new MriSchedule(null, 11L, "MRI-02", TEN.plusMinutes(10), null, 30);
        when(repository.createSchedule(expected)).thenReturn(
                new MriSchedule(2L, 11L, "MRI-02", TEN.plusMinutes(10), null, 30));

        MriSchedule result = new ScheduleService(repository).create(
                new MriSchedule(null, 11L, " MRI-02 ", TEN.plusMinutes(10), "   ", 30));

        assertThat(result.scannerRoom()).isEqualTo("MRI-02");
        assertThat(result.technologist()).isNull();
        verify(repository).createSchedule(expected);
    }

    @Test
    void updateExcludesCurrentScheduleFromConflictCheck() {
        ExamOrderRepository repository = requestedRepository();
        MriSchedule current = new MriSchedule(5L, 11L, "MRI-01", TEN, "张某", 30);
        when(repository.findSchedule(5L)).thenReturn(Optional.of(current));
        when(repository.listSchedulesForConflict()).thenReturn(List.of(current));
        when(repository.updateSchedule(current)).thenReturn(current);

        assertThat(new ScheduleService(repository).update(5L, current)).isEqualTo(current);
    }

    @Test
    void durationMustBeWithinFifteenAndOneHundredEightyMinutes() {
        ExamOrderRepository repository = requestedRepository();
        ScheduleService service = new ScheduleService(repository);

        assertThatThrownBy(() -> service.create(
                new MriSchedule(null, 11L, "MRI-01", TEN, "张某", 14)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("15—180");
        assertThatThrownBy(() -> service.create(
                new MriSchedule(null, 11L, "MRI-01", TEN, "张某", 181)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("15—180");
    }

    @Test
    void onlyRequestedExamCanBeScheduled() {
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(
                new ExamOrder(11L, 3L, "头颅MRI", "头痛", "普通", "IN_PROGRESS", null)));

        assertThatThrownBy(() -> new ScheduleService(repository).create(
                new MriSchedule(null, 11L, "MRI-01", TEN, "张某", 30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("待检查");
    }

    private static ExamOrderRepository requestedRepository() {
        ExamOrderRepository repository = mock(ExamOrderRepository.class);
        when(repository.findById(11L)).thenReturn(Optional.of(
                new ExamOrder(11L, 3L, "头颅MRI", "头痛", "普通", "REQUESTED", null)));
        when(repository.listSchedulesForConflict()).thenReturn(List.of());
        return repository;
    }
}
