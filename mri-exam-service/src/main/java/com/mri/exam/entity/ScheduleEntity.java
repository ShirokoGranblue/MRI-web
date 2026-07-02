package com.mri.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mri.common.domain.BaseEntity;

import java.time.LocalDateTime;

@TableName("mri_schedule")
public class ScheduleEntity extends BaseEntity {
    private Long examOrderId;
    private String scannerRoom;
    private LocalDateTime scheduledAt;
    private String technologist;
    private Integer durationMinutes;

    public Long getExamOrderId() {
        return examOrderId;
    }

    public void setExamOrderId(Long examOrderId) {
        this.examOrderId = examOrderId;
    }

    public String getScannerRoom() {
        return scannerRoom;
    }

    public void setScannerRoom(String scannerRoom) {
        this.scannerRoom = scannerRoom;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getTechnologist() {
        return technologist;
    }

    public void setTechnologist(String technologist) {
        this.technologist = technologist;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
