package com.mri.image.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mri.common.domain.BaseEntity;

import java.time.LocalDateTime;

@TableName("mri_download_log")
public class DownloadLogEntity extends BaseEntity {
    private Long studyId;
    private String operator;
    private String reason;
    private LocalDateTime downloadedAt;

    public Long getStudyId() {
        return studyId;
    }

    public void setStudyId(Long studyId) {
        this.studyId = studyId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloadedAt(LocalDateTime downloadedAt) {
        this.downloadedAt = downloadedAt;
    }
}
