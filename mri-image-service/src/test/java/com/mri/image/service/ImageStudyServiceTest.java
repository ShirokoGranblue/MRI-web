package com.mri.image.service;

import com.mri.image.client.ExamClient;
import com.mri.image.dto.ArchiveStudyRequest;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriSeries;
import com.mri.image.model.MriStudy;
import com.mri.image.repository.ImageStudyRepository;
import com.mri.image.storage.MinioImageStorage;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageStudyServiceTest {
    @Test
    void studyDetailIsCachedAndEvictedAfterUpdate() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = new StudyCache();
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage minioStorage = mock(MinioImageStorage.class);
        MriStudy first = new MriStudy(5L, 12L, "STUDY-001", "头颅MRI", "ARCHIVED");
        MriStudy updated = new MriStudy(5L, 12L, "STUDY-001", "头颅MRI增强", "ARCHIVED");
        when(repository.findStudyById(5L)).thenReturn(Optional.of(first), Optional.of(updated));
        when(repository.updateStudy(updated)).thenReturn(updated);

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, minioStorage);
        assertThat(service.findStudy(5L).description()).isEqualTo("头颅MRI");
        assertThat(service.findStudy(5L).description()).isEqualTo("头颅MRI");
        service.updateStudy(updated);
        assertThat(service.findStudy(5L).description()).isEqualTo("头颅MRI增强");

        verify(repository, times(2)).findStudyById(5L);
    }

    @Test
    void viewerManifestGroupsSeriesAndFilesForStudy() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = new StudyCache();
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage minioStorage = mock(MinioImageStorage.class);
        MriStudy study = new MriStudy(5L, 12L, "STUDY-001", "头颅MRI", "ARCHIVED");
        when(repository.findStudyById(5L)).thenReturn(Optional.of(study));
        when(repository.findSeriesByStudyId(5L)).thenReturn(List.of(
                new MriSeries(21L, 5L, "T1", "AXIAL"),
                new MriSeries(22L, 5L, "T2", "SAGITTAL")
        ));
        when(repository.findFilesByStudyId(5L)).thenReturn(List.of(
                new ImageFile(101L, 21L, "t1-001.dcm", "/storage/t1-001.dcm", "abc"),
                new ImageFile(102L, 22L, "t2-001.dcm", "/storage/t2-001.dcm", "def")
        ));

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, minioStorage);
        ViewerManifest manifest = service.viewerManifest(5L, "医院MRI影像系统", true);

        assertThat(manifest.watermark()).isEqualTo("医院MRI影像系统");
        assertThat(manifest.downloadEnabled()).isTrue();
        assertThat(manifest.series()).hasSize(2);
        assertThat(manifest.series().get(0).files()).extracting(ImageFile::fileName).containsExactly("t1-001.dcm");
    }

    @Test
    void archiveStudyChecksExamBeforeSaving() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = new StudyCache();
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage minioStorage = mock(MinioImageStorage.class);
        ArchiveStudyRequest request = new ArchiveStudyRequest(12L, "STUDY-001", "头颅MRI");
        when(examClient.examExists(12L)).thenReturn(true);
        when(examClient.examStatus(12L)).thenReturn("COMPLETED");
        when(repository.archive(request)).thenReturn(new MriStudy(5L, 12L, "STUDY-001", "头颅MRI", "ARCHIVED"));

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, minioStorage);

        assertThat(service.archive(request).studyInstanceUid()).isEqualTo("STUDY-001");
        verify(examClient).examExists(12L);
    }

    @Test
    void archiveRequiresCompletedExam() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = new StudyCache();
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage minioStorage = mock(MinioImageStorage.class);
        ArchiveStudyRequest req = new ArchiveStudyRequest(12L, "STUDY-001", "头颅MRI");
        when(examClient.examExists(12L)).thenReturn(true);
        when(examClient.examStatus(12L)).thenReturn("REQUESTED");

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, minioStorage);

        assertThatThrownBy(() -> service.archive(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("完成");
        verify(repository, never()).archive(req);
    }

    @Test
    void archiveGeneratesStudyInstanceUidWhenMissing() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = new StudyCache();
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage minioStorage = mock(MinioImageStorage.class);
        ArchiveStudyRequest request = new ArchiveStudyRequest(12L, null, "头颅MRI");
        when(examClient.examExists(12L)).thenReturn(true);
        when(examClient.examStatus(12L)).thenReturn("COMPLETED");
        when(repository.archive(any(ArchiveStudyRequest.class))).thenAnswer(inv -> {
            ArchiveStudyRequest r = inv.getArgument(0);
            return new MriStudy(5L, 12L, r.studyInstanceUid(), "头颅MRI", "ARCHIVED");
        });

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, minioStorage);
        MriStudy study = service.archive(request);

        assertThat(study.studyInstanceUid()).isNotNull().isNotBlank().startsWith("1.2.840.113619.");
        org.mockito.ArgumentCaptor<ArchiveStudyRequest> captor = org.mockito.ArgumentCaptor.forClass(ArchiveStudyRequest.class);
        verify(repository).archive(captor.capture());
        assertThat(captor.getValue().studyInstanceUid()).isNotNull().isNotBlank();
    }

    @Test
    void uploadFileStoresObjectAndRecordsRow() throws Exception {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = mock(StudyCache.class);
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage storage = mock(MinioImageStorage.class);
        MockMultipartFile file = new MockMultipartFile("file", "scan-001.png", "image/png", new byte[]{1, 2, 3});
        when(repository.findSeries(21L)).thenReturn(Optional.of(new MriSeries(21L, 5L, "T1", "AXIAL")));
        when(repository.createFile(any(ImageFile.class))).thenAnswer(inv -> {
            ImageFile f = inv.getArgument(0);
            return new ImageFile(7L, f.seriesId(), f.fileName(), f.storagePath(), f.checksum());
        });

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, storage);
        ImageFile saved = service.uploadFile(5L, 21L, file);

        assertThat(saved.id()).isEqualTo(7L);
        assertThat(saved.seriesId()).isEqualTo(21L);
        assertThat(saved.fileName()).isEqualTo("scan-001.png");
        assertThat(saved.storagePath()).startsWith("series/21/");
        verify(storage).putObject(eq(saved.storagePath()), any(), eq(3L), eq("image/png"));
        verify(repository).createFile(any(ImageFile.class));
        verify(cache).evict(5L);
    }

    @Test
    void uploadInfersImageMimeWhenMultipartUsesGenericBinaryType() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = mock(StudyCache.class);
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage storage = mock(MinioImageStorage.class);
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan-001.png", "application/octet-stream", new byte[]{1, 2, 3});
        when(repository.findSeries(21L)).thenReturn(Optional.of(new MriSeries(21L, 5L, "T1", "AXIAL")));
        when(repository.createFile(any(ImageFile.class))).thenAnswer(inv -> inv.getArgument(0));

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, storage);
        service.uploadFile(5L, 21L, file);

        verify(storage).putObject(any(), any(), eq(3L), eq("image/png"));
    }

    @Test
    void uploadRejectsSeriesFromDifferentStudyBeforeWritingObject() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = mock(StudyCache.class);
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage storage = mock(MinioImageStorage.class);
        MockMultipartFile file = new MockMultipartFile("file", "scan-001.png", "image/png", new byte[]{1, 2, 3});
        when(repository.findSeries(21L)).thenReturn(Optional.of(new MriSeries(21L, 9L, "T1", "AXIAL")));

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, storage);

        assertThatThrownBy(() -> service.uploadFile(5L, 21L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不属于");
        verify(storage, never()).putObject(any(), any(), any(Long.class), any());
        verify(repository, never()).createFile(any(ImageFile.class));
    }

    @Test
    void createFileRejectsSeriesFromDifferentStudy() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = mock(StudyCache.class);
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage storage = mock(MinioImageStorage.class);
        ImageFile file = new ImageFile(null, 21L, "scan-001.png", "series/21/scan-001.png", "checksum");
        when(repository.findSeries(21L)).thenReturn(Optional.of(new MriSeries(21L, 9L, "T1", "AXIAL")));

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, storage);

        assertThatThrownBy(() -> service.createFile(file, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不属于");
        verify(repository, never()).createFile(file);
    }

    @Test
    void deleteFileRemovesObjectThenRow() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = mock(StudyCache.class);
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage storage = mock(MinioImageStorage.class);
        ImageFile existing = new ImageFile(101L, 21L, "a.png", "series/21/a.png", "x");
        when(repository.findFile(101L)).thenReturn(Optional.of(existing));
        when(repository.findSeries(21L)).thenReturn(Optional.of(new MriSeries(21L, 5L, "T1", "AXIAL")));

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, storage);
        service.deleteFile(101L);

        InOrder inOrder = inOrder(storage, repository);
        inOrder.verify(storage).removeObjectQuietly("series/21/a.png");
        inOrder.verify(repository).deleteFile(101L);
        verify(cache).evict(5L);
    }

    @Test
    void deleteStudyCascadesSeriesFilesAndObjects() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = mock(StudyCache.class);
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage storage = mock(MinioImageStorage.class);
        when(repository.findFilesByStudyId(5L)).thenReturn(List.of(
                new ImageFile(101L, 21L, "a.png", "series/21/a.png", "x"),
                new ImageFile(102L, 22L, "b.png", "series/22/b.png", "y")));

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, storage);
        service.deleteStudy(5L);

        verify(storage).removeObjectQuietly("series/21/a.png");
        verify(storage).removeObjectQuietly("series/22/b.png");
        verify(repository).deleteFilesByStudyId(5L);
        verify(repository).deleteSeriesByStudyId(5L);
        verify(repository).deleteStudy(5L);
        verify(cache).evict(5L);
    }

    @Test
    void deleteSeriesCascadesFilesAndObjects() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = mock(StudyCache.class);
        ExamClient examClient = mock(ExamClient.class);
        MinioImageStorage storage = mock(MinioImageStorage.class);
        when(repository.findSeries(21L)).thenReturn(Optional.of(new MriSeries(21L, 5L, "T1", "AXIAL")));
        when(repository.findFilesBySeriesId(21L)).thenReturn(List.of(
                new ImageFile(101L, 21L, "a.png", "series/21/a.png", "x")));

        ImageStudyService service = new ImageStudyService(repository, cache, examClient, storage);
        service.deleteSeries(21L);

        verify(storage).removeObjectQuietly("series/21/a.png");
        verify(repository).deleteFilesBySeriesId(21L);
        verify(repository).deleteSeries(21L);
        verify(cache).evict(5L);
    }
}
