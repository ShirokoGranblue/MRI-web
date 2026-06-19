package com.mri.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mri.common.domain.BaseEntity;

@TableName("mri_exam_order")
public class ExamOrderEntity extends BaseEntity {
    private Long patientId;
    private String examItem;
    private String clinicalDiagnosis;
    private String priority;
    private String status;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getExamItem() {
        return examItem;
    }

    public void setExamItem(String examItem) {
        this.examItem = examItem;
    }

    public String getClinicalDiagnosis() {
        return clinicalDiagnosis;
    }

    public void setClinicalDiagnosis(String clinicalDiagnosis) {
        this.clinicalDiagnosis = clinicalDiagnosis;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
