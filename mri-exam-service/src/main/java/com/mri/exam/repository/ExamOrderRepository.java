package com.mri.exam.repository;

import com.mri.common.api.PageResult;
import com.mri.exam.dto.CreateExamOrderRequest;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.model.MriSchedule;
import com.mri.exam.model.RiskAssessment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamOrderRepository {
    ExamOrder save(CreateExamOrderRequest request);

    default ExamOrder save(CreateExamOrderRequest request, RiskAssessment risk) {
        ExamOrder saved = save(request);
        return updateRisk(saved.id(), risk, null, null);
    }

    Optional<ExamOrder> findById(Long id);

    PageResult<ExamOrder> page(long page, long size, String status);

    List<ExamOrder> listByPatient(Long patientId);

    ExamOrder update(Long id, CreateExamOrderRequest request);

    default ExamOrder update(Long id, CreateExamOrderRequest request, RiskAssessment risk) {
        update(id, request);
        return updateRisk(id, risk, null, null);
    }

    ExamOrder updateRisk(Long id, RiskAssessment risk, String confirmedBy, LocalDateTime confirmedAt);

    ExamOrder cancel(Long id);

    void delete(Long id);

    ExamOrder updateStatus(Long id, String status);

    MriSchedule createSchedule(MriSchedule schedule);

    MriSchedule updateSchedule(MriSchedule schedule);

    Optional<MriSchedule> findSchedule(Long id);

    List<MriSchedule> listSchedules(Long examOrderId);

    List<MriSchedule> listSchedulesForConflict();

    void deleteSchedule(Long id);
}
