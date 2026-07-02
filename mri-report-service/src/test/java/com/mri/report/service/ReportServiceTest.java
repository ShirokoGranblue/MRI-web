package com.mri.report.service;

import com.mri.report.client.ExamClient;
import com.mri.report.client.ImageClient;
import com.mri.report.dto.CreateReportRequest;
import com.mri.report.model.Report;
import com.mri.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportServiceTest {
    @Test
    void publishReportUsesStudyInformationAndUpdatesExamStatus() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        Report approved = new Report(31L, 12L, 5L, "双侧基底节区未见异常信号", null, "APPROVED");
        when(repository.findById(31L)).thenReturn(Optional.of(approved));
        when(imageClient.studyDescription(5L)).thenReturn("头颅MRI");
        when(repository.updateStatus(31L, "PUBLISHED")).thenReturn(new Report(31L, 12L, 5L, "双侧基底节区未见异常信号", null, "PUBLISHED"));

        ReportService service = new ReportService(repository, imageClient, examClient);
        Report report = service.publish(31L, "admin");

        assertThat(report.status()).isEqualTo("PUBLISHED");
        assertThat(report.findings()).isEqualTo("双侧基底节区未见异常信号");
        verify(imageClient).studyDescription(5L);
        verify(examClient).markReported(12L);
        verify(repository).audit(31L, "PUBLISH", "admin", "发布报告，Study=头颅MRI");
    }

    @Test
    void publishRejectsDraftReport() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        when(repository.findById(31L)).thenReturn(Optional.of(new Report(31L, 12L, 5L, "双侧基底节区未见异常信号", null, "DRAFT")));

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThatThrownBy(() -> service.publish(31L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已审核");
        verify(repository, never()).updateStatus(31L, "PUBLISHED");
        verifyNoInteractions(imageClient, examClient);
    }

    @Test
    void approveRejectsDraftReport() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        when(repository.findById(31L)).thenReturn(Optional.of(new Report(31L, 12L, 5L, "双侧基底节区未见异常信号", null, "DRAFT")));

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThatThrownBy(() -> service.approve(31L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("待审核");
        verify(repository, never()).updateStatus(31L, "APPROVED");
    }

    @Test
    void createReportStartsAsDraft() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        CreateReportRequest request = new CreateReportRequest(12L, 5L, "未见异常", "建议随访");
        when(examClient.examStatus(12L)).thenReturn("COMPLETED");
        when(imageClient.studyExamOrderId(5L)).thenReturn(12L);
        when(repository.createDraft(request)).thenReturn(new Report(31L, 12L, 5L, "未见异常", "建议随访", "DRAFT"));

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThat(service.create(request).status()).isEqualTo("DRAFT");
    }

    @Test
    void createRejectsStudyFromDifferentExam() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        CreateReportRequest request = new CreateReportRequest(12L, 5L, "所见", "意见");
        when(examClient.examStatus(12L)).thenReturn("COMPLETED");
        when(imageClient.studyExamOrderId(5L)).thenReturn(99L);

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不属于");
        verify(repository, never()).createDraft(request);
    }

    @Test
    void updateRejectsStudyFromDifferentExam() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        CreateReportRequest request = new CreateReportRequest(12L, 5L, "修改所见", "修改意见");
        when(repository.findById(31L)).thenReturn(Optional.of(
                new Report(31L, 12L, 5L, "原所见", "原意见", "DRAFT")
        ));
        when(examClient.examStatus(12L)).thenReturn("COMPLETED");
        when(imageClient.studyExamOrderId(5L)).thenReturn(99L);

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThatThrownBy(() -> service.update(31L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不属于");
        verify(repository, never()).update(31L, request);
    }

    @Test
    void createRejectsUnlessExamCompleted() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        CreateReportRequest request = new CreateReportRequest(12L, 5L, "所见", "意见");
        when(examClient.examStatus(12L)).thenReturn("IN_PROGRESS");

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("完成");
        verify(repository, never()).createDraft(request);
    }

    @Test
    void reopenReturnsRejectedToDraft() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        when(repository.findById(31L)).thenReturn(Optional.of(new Report(31L, 12L, 5L, "所见", "意见", "REJECTED")));
        when(repository.updateStatus(31L, "DRAFT")).thenReturn(new Report(31L, 12L, 5L, "所见", "意见", "DRAFT"));

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThat(service.reopen(31L, "admin").status()).isEqualTo("DRAFT");
        verify(repository).audit(31L, "REOPEN", "admin", "回到草稿修改");
    }

    @Test
    void updateAndDeleteRejectPublishedReport() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        CreateReportRequest request = new CreateReportRequest(12L, 5L, "修改所见", "修改意见");
        when(repository.findById(31L)).thenReturn(Optional.of(
                new Report(31L, 12L, 5L, "原所见", "原意见", "PUBLISHED")
        ));

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThatThrownBy(() -> service.update(31L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已发布");
        assertThatThrownBy(() -> service.delete(31L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已发布");
        verify(repository, never()).update(31L, request);
        verify(repository, never()).delete(31L);
    }

    @Test
    void updateAndDeleteRejectApprovedReport() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        CreateReportRequest request = new CreateReportRequest(12L, 5L, "审核后修改", "审核后修改");
        when(repository.findById(31L)).thenReturn(Optional.of(
                new Report(31L, 12L, 5L, "已审核所见", "已审核意见", "APPROVED")
        ));

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThatThrownBy(() -> service.update(31L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("草稿或已驳回");
        assertThatThrownBy(() -> service.delete(31L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("草稿或已驳回");
        verify(repository, never()).update(31L, request);
        verify(repository, never()).delete(31L);
        verifyNoInteractions(imageClient, examClient);
    }

    @Test
    void submitRejectsMissingAuthenticatedOperator() {
        ReportRepository repository = mock(ReportRepository.class);
        ImageClient imageClient = mock(ImageClient.class);
        ExamClient examClient = mock(ExamClient.class);
        when(repository.findById(31L)).thenReturn(Optional.of(
                new Report(31L, 12L, 5L, "所见", "意见", "DRAFT")
        ));

        ReportService service = new ReportService(repository, imageClient, examClient);

        assertThatThrownBy(() -> service.submit(31L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("登录用户");
        verify(repository, never()).updateStatus(31L, "SUBMITTED");
    }
}
