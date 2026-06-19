package com.mri.image.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mri.common.domain.BaseEntity;

@TableName("mri_series")
public class MriSeriesEntity extends BaseEntity {
    private Long studyId;
    private String seriesName;
    private String bodyPosition;

    public Long getStudyId() {
        return studyId;
    }

    public void setStudyId(Long studyId) {
        this.studyId = studyId;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getBodyPosition() {
        return bodyPosition;
    }

    public void setBodyPosition(String bodyPosition) {
        this.bodyPosition = bodyPosition;
    }
}
