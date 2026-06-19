package com.mri.image.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mri.common.domain.BaseEntity;

@TableName("mri_study")
public class MriStudyEntity extends BaseEntity {
    private Long examOrderId;
    private String studyInstanceUid;
    private String description;
    private String status;

    public Long getExamOrderId() {
        return examOrderId;
    }

    public void setExamOrderId(Long examOrderId) {
        this.examOrderId = examOrderId;
    }

    public String getStudyInstanceUid() {
        return studyInstanceUid;
    }

    public void setStudyInstanceUid(String studyInstanceUid) {
        this.studyInstanceUid = studyInstanceUid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
