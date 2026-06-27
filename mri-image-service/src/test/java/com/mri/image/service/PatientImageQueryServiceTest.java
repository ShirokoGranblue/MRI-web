package com.mri.image.service;

import com.mri.image.mapper.PatientImageAccessMapper;
import com.mri.image.model.ImageFile;
import com.mri.image.model.MriStudy;
import com.mri.image.repository.ImageStudyRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientImageQueryServiceTest {
    @Test
    void listsOnlyOwnedStudiesAndShowsPublicationState() {
        PatientImageAccessMapper access = mock(PatientImageAccessMapper.class);
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        MriStudy study = new MriStudy(31L, 21L, "1.2.3", "头颅 MRI", "ARCHIVED");
        when(access.findStudyIdsByUsername("patient01")).thenReturn(List.of(31L));
        when(access.isReportPublished(31L, "patient01")).thenReturn(true);
        when(repository.findStudyById(31L)).thenReturn(Optional.of(study));
        when(repository.findFilesByStudyId(31L)).thenReturn(List.of(
                new ImageFile(41L, 51L, "scan.png", "series/scan.png", "abc")
        ));

        PatientImageQueryService service = new PatientImageQueryService(access, repository);

        assertThat(service.findMine("patient01")).singleElement()
                .satisfies(view -> {
                    assertThat(view.study()).isEqualTo(study);
                    assertThat(view.fileCount()).isEqualTo(1);
                    assertThat(view.reportPublished()).isTrue();
                });
    }

    @Test
    void blocksManifestAndFileUntilPublished() {
        PatientImageAccessMapper access = mock(PatientImageAccessMapper.class);
        ImageStudyRepository repository = mock(ImageStudyRepository.class);
        when(access.ownsStudy(31L, "patient01")).thenReturn(true);
        when(access.isReportPublished(31L, "patient01")).thenReturn(false);
        when(access.findStudyIdByFile(41L, "patient01")).thenReturn(31L);
        PatientImageQueryService service = new PatientImageQueryService(access, repository);

        assertThatThrownBy(() -> service.assertStudyVisible(31L, "patient01"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("诊断报告发布后方可查看影像");
        assertThatThrownBy(() -> service.assertFileVisible(41L, "patient01"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("诊断报告发布后方可查看影像");
    }
}
