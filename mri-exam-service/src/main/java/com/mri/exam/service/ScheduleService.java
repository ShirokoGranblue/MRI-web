package com.mri.exam.service;

import com.mri.common.exception.ConflictException;
import com.mri.exam.model.ExamOrder;
import com.mri.exam.model.MriSchedule;
import com.mri.exam.repository.ExamOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ScheduleService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final ExamOrderRepository repository;

    public ScheduleService(ExamOrderRepository repository) {
        this.repository = repository;
    }

    public MriSchedule create(MriSchedule schedule) {
        MriSchedule normalized = validateAndNormalize(null, schedule);
        return repository.createSchedule(normalized);
    }

    public MriSchedule update(Long id, MriSchedule schedule) {
        repository.findSchedule(id).orElseThrow(() -> new IllegalArgumentException("检查排程不存在"));
        MriSchedule normalized = validateAndNormalize(id, new MriSchedule(
                id,
                schedule.examOrderId(),
                schedule.scannerRoom(),
                schedule.scheduledAt(),
                schedule.technologist(),
                schedule.durationMinutes()
        ));
        return repository.updateSchedule(normalized);
    }

    private MriSchedule validateAndNormalize(Long currentId, MriSchedule schedule) {
        if (schedule.examOrderId() == null) {
            throw new IllegalArgumentException("请选择检查申请");
        }
        ExamOrder order = repository.findById(schedule.examOrderId())
                .orElseThrow(() -> new IllegalArgumentException("检查申请不存在"));
        if (!"REQUESTED".equals(order.status())) {
            throw new IllegalArgumentException("仅待检查状态的检查申请可以安排排程");
        }

        String room = requiredTrimmed(schedule.scannerRoom(), "检查室不能为空");
        if (schedule.scheduledAt() == null) {
            throw new IllegalArgumentException("排程开始时间不能为空");
        }
        int duration = schedule.durationMinutes() == null ? 30 : schedule.durationMinutes();
        if (duration < 15 || duration > 180) {
            throw new IllegalArgumentException("检查时长必须在 15—180 分钟之间");
        }
        String technologist = blankToNull(schedule.technologist());
        MriSchedule normalized = new MriSchedule(
                currentId,
                schedule.examOrderId(),
                room,
                schedule.scheduledAt(),
                technologist,
                duration
        );
        rejectConflicts(normalized);
        return normalized;
    }

    private void rejectConflicts(MriSchedule incoming) {
        LocalDateTime incomingEnd = incoming.scheduledAt().plusMinutes(incoming.durationMinutes());
        for (MriSchedule existing : repository.listSchedulesForConflict()) {
            if (incoming.id() != null && incoming.id().equals(existing.id())) {
                continue;
            }
            int existingDuration = existing.durationMinutes() == null ? 30 : existing.durationMinutes();
            LocalDateTime existingEnd = existing.scheduledAt().plusMinutes(existingDuration);
            if (!overlaps(incoming.scheduledAt(), incomingEnd, existing.scheduledAt(), existingEnd)) {
                continue;
            }
            String interval = existing.scheduledAt().format(TIME_FORMAT) + "—" + existingEnd.format(TIME_FORMAT);
            if (incoming.scannerRoom().equals(existing.scannerRoom())) {
                throw new ConflictException(incoming.scannerRoom() + " 在 " + interval
                        + " 已由检查申请 " + existing.examOrderId() + " 安排，请调整检查室或时间");
            }
            if (incoming.technologist() != null && incoming.technologist().equals(blankToNull(existing.technologist()))) {
                throw new ConflictException("技师 " + incoming.technologist() + " 在 " + interval
                        + " 已为检查申请 " + existing.examOrderId() + " 安排检查");
            }
        }
    }

    private boolean overlaps(LocalDateTime leftStart, LocalDateTime leftEnd,
                             LocalDateTime rightStart, LocalDateTime rightEnd) {
        return leftStart.isBefore(rightEnd) && rightStart.isBefore(leftEnd);
    }

    private String requiredTrimmed(String value, String message) {
        String result = blankToNull(value);
        if (result == null) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
