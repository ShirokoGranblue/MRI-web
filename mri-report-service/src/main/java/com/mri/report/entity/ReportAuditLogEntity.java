package com.mri.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mri.common.domain.BaseEntity;

import java.time.LocalDateTime;

@TableName("mri_report_audit_log")
public class ReportAuditLogEntity extends BaseEntity {
    private Long reportId;
    private String action;
    private String operator;
    private String comment;
    private LocalDateTime operatedAt;

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getOperatedAt() {
        return operatedAt;
    }

    public void setOperatedAt(LocalDateTime operatedAt) {
        this.operatedAt = operatedAt;
    }
}
