package com.mri.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mri.common.domain.BaseEntity;

@TableName("mri_report")
public class ReportEntity extends BaseEntity {
    private Long examOrderId;
    private Long studyId;
    private String findings;
    private String impression;
    private String status;

    public Long getExamOrderId() {
        return examOrderId;
    }

    public void setExamOrderId(Long examOrderId) {
        this.examOrderId = examOrderId;
    }

    public Long getStudyId() {
        return studyId;
    }

    public void setStudyId(Long studyId) {
        this.studyId = studyId;
    }

    public String getFindings() {
        return findings;
    }

    public void setFindings(String findings) {
        this.findings = findings;
    }

    public String getImpression() {
        return impression;
    }

    public void setImpression(String impression) {
        this.impression = impression;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
