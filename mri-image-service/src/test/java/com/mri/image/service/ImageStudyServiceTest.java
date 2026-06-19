package com.mri.image.service;

import com.mri.image.client.ExamClient;
import com.mri.image.dto.ArchiveStudyRequest;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriSeries;
import com.mri.image.model.MriStudy;
import com.mri.image.repository.ImageStudyRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageStudyServiceTest {
    @Test
    void studyDetailIsCachedAndEvictedAfterUpdate() {
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        StudyCache cache = new StudyCache();
        ExamClient examClient = mock(ExamClient.class);
        MriStudy first = new MriStudy(5L, 12L, "STUDY-001", "头颅MRI", "ARCHIVED");
        MriStudy updated = new MriStudy(5L, 12L, "STUDY-001", "头颅MRI增强", "ARCHIVED");
        when(repository.findStudyById(5L)).thenReturn(Optional.of(first), Optional.of(updated));
        when(repository.updateStudy(updated)).thenReturn(updated);

        ImageStudyService service = new ImageStudyService(repository, cache, examClient);
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

        ImageStudyService service = new ImageStudyService(repository, cache, examClient);
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
        ArchiveStudyRequest request = new ArchiveStudyRequest(12L, "STUDY-001", "头颅MRI");
        when(examClient.examExists(12L)).thenReturn(true);
        when(repository.archive(request)).thenReturn(new MriStudy(5L, 12L, "STUDY-001", "头颅MRI", "ARCHIVED"));

        ImageStudyService service = new ImageStudyService(repository, cache, examClient);

        assertThat(service.archive(request).studyInstanceUid()).isEqualTo("STUDY-001");
        verify(examClient).examExists(12L);
    }
}
