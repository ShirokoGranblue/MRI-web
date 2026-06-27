package com.mri.report.service;

import com.mri.report.entity.ReportEntity;
import com.mri.report.mapper.PatientReportAccessMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientReportQueryServiceTest {
    @Test
    void hidesReportContentUntilPublished() {
        PatientReportAccessMapper access = mock(PatientReportAccessMapper.class);
        ReportEntity draft = report(1L, "SUBMITTED", "未发布所见", "未发布意见");
        ReportEntity published = report(2L, "PUBLISHED", "已发布所见", "已发布意见");
        when(access.findReportsByUsername("patient01")).thenReturn(List.of(draft, published));

        PatientReportQueryService service = new PatientReportQueryService(access);
        var reports = service.findMine("patient01");

        assertThat(reports.get(0).findings()).isNull();
        assertThat(reports.get(0).impression()).isNull();
        assertThat(reports.get(1).findings()).isEqualTo("已发布所见");
        assertThat(reports.get(1).impression()).isEqualTo("已发布意见");
    }

    private static ReportEntity report(Long id, String status, String findings, String impression) {
        ReportEntity entity = new ReportEntity();
        entity.setId(id);
        entity.setExamOrderId(id + 10);
        entity.setStudyId(id + 20);
        entity.setStatus(status);
        entity.setFindings(findings);
        entity.setImpression(impression);
        return entity;
    }
}
