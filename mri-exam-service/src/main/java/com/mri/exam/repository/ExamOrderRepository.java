package com.mri.exam.repository;

import com.mri.common.api.PageResult;
import com.mri.exam.dto.CreateExamOrderRequest;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.model.MriSchedule;

import java.util.List;
import java.util.Optional;

public interface ExamOrderRepository {
    ExamOrder save(CreateExamOrderRequest request);

    Optional<ExamOrder> findById(Long id);

    PageResult<ExamOrder> page(long page, long size, String status);

    List<ExamOrder> listByPatient(Long patientId);

    ExamOrder update(Long id, CreateExamOrderRequest request);

    ExamOrder cancel(Long id);

    void delete(Long id);

    ExamOrder updateStatus(Long id, String status);

    MriSchedule createSchedule(MriSchedule schedule);

    MriSchedule updateSchedule(MriSchedule schedule);

    Optional<MriSchedule> findSchedule(Long id);

    List<MriSchedule> listSchedules(Long examOrderId);

    void deleteSchedule(Long id);
}
