package com.mri.exam.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mri.common.api.PageResult;
import com.mri.exam.dto.CreateExamOrderRequest;
import com.mri.exam.entity.ExamOrderEntity;
import com.mri.exam.entity.ScheduleEntity;
import com.mri.exam.mapper.ExamOrderMapper;
import com.mri.exam.mapper.ScheduleMapper;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.model.MriSchedule;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisExamOrderRepository implements ExamOrderRepository {
    private final ExamOrderMapper examOrderMapper;
    private final ScheduleMapper scheduleMapper;

    public MybatisExamOrderRepository(ExamOrderMapper examOrderMapper, ScheduleMapper scheduleMapper) {
        this.examOrderMapper = examOrderMapper;
        this.scheduleMapper = scheduleMapper;
    }

    @Override
    public ExamOrder save(CreateExamOrderRequest request) {
        ExamOrderEntity entity = new ExamOrderEntity();
        entity.setPatientId(request.patientId());
        entity.setExamItem(request.examItem());
        entity.setClinicalDiagnosis(request.clinicalDiagnosis());
        entity.setPriority(request.priority());
        entity.setStatus("REQUESTED");
        examOrderMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public Optional<ExamOrder> findById(Long id) {
        return Optional.ofNullable(examOrderMapper.selectById(id)).map(MybatisExamOrderRepository::toModel);
    }

    @Override
    public PageResult<ExamOrder> page(long page, long size, String status) {
        LambdaQueryWrapper<ExamOrderEntity> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(ExamOrderEntity::getStatus, status);
        }
        Page<ExamOrderEntity> result = examOrderMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(page, size, result.getTotal(), result.getRecords().stream().map(MybatisExamOrderRepository::toModel).toList());
    }

    @Override
    public ExamOrder update(Long id, CreateExamOrderRequest request) {
        ExamOrderEntity entity = examOrderMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("检查申请不存在");
        }
        entity.setPatientId(request.patientId());
        entity.setExamItem(request.examItem());
        entity.setClinicalDiagnosis(request.clinicalDiagnosis());
        entity.setPriority(request.priority());
        examOrderMapper.updateById(entity);
        return toModel(entity);
    }

    @Override
    public void cancel(Long id) {
        updateStatus(id, "CANCELLED");
    }

    @Override
    public ExamOrder updateStatus(Long id, String status) {
        ExamOrderEntity entity = examOrderMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("检查申请不存在");
        }
        entity.setStatus(status);
        examOrderMapper.updateById(entity);
        return toModel(entity);
    }

    @Override
    public MriSchedule createSchedule(MriSchedule schedule) {
        ScheduleEntity entity = toEntity(schedule);
        scheduleMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public MriSchedule updateSchedule(MriSchedule schedule) {
        scheduleMapper.updateById(toEntity(schedule));
        return schedule;
    }

    @Override
    public Optional<MriSchedule> findSchedule(Long id) {
        return Optional.ofNullable(scheduleMapper.selectById(id)).map(MybatisExamOrderRepository::toModel);
    }

    @Override
    public List<MriSchedule> listSchedules(Long examOrderId) {
        return scheduleMapper.selectList(new LambdaQueryWrapper<ScheduleEntity>().eq(ScheduleEntity::getExamOrderId, examOrderId))
                .stream().map(MybatisExamOrderRepository::toModel).toList();
    }

    @Override
    public void deleteSchedule(Long id) {
        scheduleMapper.deleteById(id);
    }

    private static ExamOrder toModel(ExamOrderEntity entity) {
        return new ExamOrder(entity.getId(), entity.getPatientId(), entity.getExamItem(), entity.getStatus());
    }

    private static MriSchedule toModel(ScheduleEntity entity) {
        return new MriSchedule(entity.getId(), entity.getExamOrderId(), entity.getScannerRoom(), entity.getScheduledAt(), entity.getTechnologist());
    }

    private static ScheduleEntity toEntity(MriSchedule schedule) {
        ScheduleEntity entity = new ScheduleEntity();
        entity.setId(schedule.id());
        entity.setExamOrderId(schedule.examOrderId());
        entity.setScannerRoom(schedule.scannerRoom());
        entity.setScheduledAt(schedule.scheduledAt());
        entity.setTechnologist(schedule.technologist());
        return entity;
    }
}
