package com.mri.image.service;

import com.mri.image.config.ImageDemoProperties;
import com.mri.image.model.DownloadLog;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriSeries;
import com.mri.image.model.MriStudy;
import com.mri.image.repository.ImageStudyRepository;
import com.mri.image.storage.MinioImageStorage;
import com.mri.image.storage.MinioImageStorage.LoadedObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageDownloadServiceTest {
    @Test
    void doctorSingleFileReturnsOriginalBytesMimeAndNameThenAuditsSuccess() {
        Fixture fixture = fixture(true);
        when(fixture.storage.loadObject("series/51/scan-中文.png"))
                .thenReturn(new LoadedObject(new byte[]{1, 2, 3}, "image/png"));

        ImageDownloadService.DownloadedFile result = fixture.service
                .downloadDoctorFile(41L, "admin", "会诊");

        assertThat(result.fileName()).isEqualTo("scan-中文.png");
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.content()).containsExactly(1, 2, 3);
        verify(fixture.repository).createDownloadLog(any(DownloadLog.class));
    }

    @Test
    void disabledDownloadRejectsBeforeReadingAndDoesNotAudit() {
        Fixture fixture = fixture(false);

        assertThatThrownBy(() -> fixture.service.downloadDoctorFile(41L, "admin", "诊断查看"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("关闭");

        verify(fixture.storage, never()).loadObject(any());
        verify(fixture.repository, never()).createDownloadLog(any());
    }

    @Test
    void otherDoctorReasonRequiresExplanation() {
        Fixture fixture = fixture(true);

        assertThatThrownBy(() -> fixture.service.downloadDoctorFile(41L, "admin", "其他"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("说明");
        verify(fixture.repository, never()).createDownloadLog(any());
    }

    @Test
    void patientSingleFileChecksOwnershipAndPublishedReportAndUsesFixedReason() {
        Fixture fixture = fixture(true);
        when(fixture.storage.loadObject("series/51/scan-中文.png"))
                .thenReturn(new LoadedObject(new byte[]{1}, "image/png"));

        fixture.service.downloadPatientFile(41L, "patient01");

        verify(fixture.patientQuery).assertFileVisible(41L, "patient01");
        verify(fixture.repository).createDownloadLog(new DownloadLog(
                null, 31L, 41L, "SINGLE", "patient01", "患者本人下载", null));
    }

    @Test
    void studyZipUsesSeriesDirectoriesAndKeepsSameNamesFromDifferentSeries() throws Exception {
        Fixture fixture = fixture(true);
        when(fixture.repository.findSeriesByStudyId(31L)).thenReturn(List.of(
                new MriSeries(51L, 31L, "T1", "HEAD"),
                new MriSeries(52L, 31L, "T2", "HEAD")
        ));
        when(fixture.repository.findFilesByStudyId(31L)).thenReturn(List.of(
                new ImageFile(41L, 51L, "scan.png", "series/51/scan.png", "a"),
                new ImageFile(42L, 52L, "scan.png", "series/52/scan.png", "b")
        ));
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            OutputStream out = invocation.getArgument(1);
            out.write(key.getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(fixture.storage).writeObject(any(), any(OutputStream.class));

        ImageDownloadService.DownloadArchive archive = fixture.service
                .downloadDoctorStudy(31L, "admin", "归档导出");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        archive.body().writeTo(output);

        assertThat(archive.fileName()).isEqualTo("Study-31-影像.zip");
        try (ZipInputStream zip = new ZipInputStream(
                new java.io.ByteArrayInputStream(output.toByteArray()), StandardCharsets.UTF_8)) {
            assertThat(zip.getNextEntry().getName()).isEqualTo("series-51/scan.png");
            assertThat(zip.getNextEntry().getName()).isEqualTo("series-52/scan.png");
        }
        verify(fixture.repository).createDownloadLog(new DownloadLog(
                null, 31L, null, "STUDY_ZIP", "admin", "归档导出", null));
    }

    @Test
    void zipStorageFailureDoesNotCreateFalseAuditLog() throws Exception {
        Fixture fixture = fixture(true);
        when(fixture.repository.findFilesByStudyId(31L)).thenReturn(List.of(
                new ImageFile(41L, 51L, "scan.png", "series/51/scan.png", "a")
        ));
        doThrow(new IllegalStateException("对象读取失败"))
                .when(fixture.storage).writeObject(any(), any(OutputStream.class));

        ImageDownloadService.DownloadArchive archive = fixture.service
                .downloadDoctorStudy(31L, "admin", "会诊");

        assertThatThrownBy(() -> archive.body().writeTo(new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class);
        verify(fixture.repository, never()).createDownloadLog(any());
    }

    @Test
    void emptyStudyCannotProduceZipOrAudit() {
        Fixture fixture = fixture(true);
        when(fixture.repository.findFilesByStudyId(31L)).thenReturn(List.of());

        assertThatThrownBy(() -> fixture.service.downloadDoctorStudy(31L, "admin", "会诊"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("没有可下载");
        verify(fixture.repository, never()).createDownloadLog(any());
    }

    private static Fixture fixture(boolean downloadEnabled) {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        MinioImageStorage storage = mock(MinioImageStorage.class);
        PatientImageQueryService patientQuery = mock(PatientImageQueryService.class);
        ImageDemoProperties properties = mock(ImageDemoProperties.class);
        when(properties.isDownloadEnabled()).thenReturn(downloadEnabled);
        when(repository.findStudyById(31L)).thenReturn(Optional.of(
                new MriStudy(31L, 21L, "1.2.3", "头颅 MRI", "ARCHIVED")));
        when(repository.findSeries(51L)).thenReturn(Optional.of(
                new MriSeries(51L, 31L, "T1", "HEAD")));
        when(repository.findFile(41L)).thenReturn(Optional.of(
                new ImageFile(41L, 51L, "scan-中文.png", "series/51/scan-中文.png", "a")));
        ImageDownloadService service = new ImageDownloadService(repository, storage, patientQuery, properties);
        return new Fixture(service, repository, storage, patientQuery);
    }

    private record Fixture(
            ImageDownloadService service,
            ImageStudyRepository repository,
            MinioImageStorage storage,
            PatientImageQueryService patientQuery
    ) {
    }
}
