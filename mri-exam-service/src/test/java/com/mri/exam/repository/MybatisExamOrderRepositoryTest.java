package com.mri.exam.repository;

import com.mri.exam.entity.ScheduleEntity;
import com.mri.exam.mapper.ExamOrderMapper;
import com.mri.exam.mapper.ScheduleMapper;
import com.mri.exam.model.MriSchedule;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MybatisExamOrderRepositoryTest {
    @Test
    void updateMissingScheduleThrows() {
        ExamOrderMapper examOrderMapper = mock(ExamOrderMapper.class);
        ScheduleMapper scheduleMapper = mock(ScheduleMapper.class);
        when(scheduleMapper.updateById(any(ScheduleEntity.class))).thenReturn(0);
        MybatisExamOrderRepository repository = new MybatisExamOrderRepository(examOrderMapper, scheduleMapper);

        MriSchedule schedule = new MriSchedule(99L, 11L, "MRI-1", LocalDateTime.of(2026, 6, 22, 10, 0), "技师A");

        assertThatThrownBy(() -> repository.updateSchedule(schedule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("检查排程不存在");
    }

    @Test
    void deleteMissingScheduleThrows() {
        ExamOrderMapper examOrderMapper = mock(ExamOrderMapper.class);
        ScheduleMapper scheduleMapper = mock(ScheduleMapper.class);
        when(scheduleMapper.deleteById((Serializable) 99L)).thenReturn(0);
        MybatisExamOrderRepository repository = new MybatisExamOrderRepository(examOrderMapper, scheduleMapper);

        assertThatThrownBy(() -> repository.deleteSchedule(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("检查排程不存在");
    }
}
