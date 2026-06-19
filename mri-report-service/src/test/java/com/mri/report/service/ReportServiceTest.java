package com.mri.report.service;

import com.mri.report.client.ExamClient;
import com.mri.report.client.ImageClient;
import com.mri.report.dto.CreateReportRequest;
import com.mri.report.model.Report;
import com.mri.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceTest {
    @Test
    void publishReportUsesStudyInformationAndUpdatesExamStatus() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        Report draft = new Report(31L, 12L, 5L, "双侧基底节区未见异常信号", "DRAFT");
        when(repository.findById(31L)).thenReturn(Optional.of(draft));
        when(imageClient.studyDescription(5L)).thenReturn("头颅MRI");
        when(repository.updateStatus(31L, "PUBLISHED")).thenReturn(new Report(31L, 12L, 5L, "双侧基底节区未见异常信号", "PUBLISHED"));

        ReportService service = new ReportService(repository, imageClient, examClient);
        Report report = service.publish(31L);

        assertThat(report.status()).isEqualTo("PUBLISHED");
        assertThat(report.findings()).isEqualTo("双侧基底节区未见异常信号");
        verify(imageClient).studyDescription(5L);
        verify(examClient).markReported(12L);
        verify(repository).audit(31L, "PUBLISH", "audit-doctor", "发布报告，Study=头颅MRI");
    }

    @Test
    void createReportStartsAsDraft() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        CreateReportRequest request = new CreateReportRequest(12L, 5L, "未见异常", "建议随访");
        when(repository.createDraft(request)).thenReturn(new Report(31L, 12L, 5L, "未见异常", "DRAFT"));

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThat(service.create(request).status()).isEqualTo("DRAFT");
    }
}
