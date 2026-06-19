package com.mri.patient.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mri.common.domain.BaseEntity;

@TableName("mri_contraindication")
public class ContraindicationEntity extends BaseEntity {
    private Long patientId;
    private String type;
    private String description;
    private String severity;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
