package com.mri.image.repository;

import com.mri.image.entity.MriStudyEntity;
import com.mri.image.mapper.DownloadLogMapper;
import com.mri.image.mapper.ImageFileMapper;
import com.mri.image.mapper.MriSeriesMapper;
import com.mri.image.mapper.MriStudyMapper;
import com.mri.image.model.MriStudy;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MybatisImageStudyRepositoryTest {
    @Test
    void updateMissingStudyThrows() {
        MriStudyMapper studyMapper = mock(MriStudyMapper.class);
        MriSeriesMapper seriesMapper = mock(MriSeriesMapper.class);
        ImageFileMapper fileMapper = mock(ImageFileMapper.class);
        DownloadLogMapper downloadLogMapper = mock(DownloadLogMapper.class);
        when(studyMapper.updateById(any(MriStudyEntity.class))).thenReturn(0);
        MybatisImageStudyRepository repository = new MybatisImageStudyRepository(studyMapper, seriesMapper, fileMapper, downloadLogMapper);

        MriStudy study = new MriStudy(99L, 11L, "1.2.3", "不存在", "ARCHIVED");

        assertThatThrownBy(() -> repository.updateStudy(study))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Study 不存在");
    }

    @Test
    void deleteMissingStudyThrows() {
        MriStudyMapper studyMapper = mock(MriStudyMapper.class);
        MriSeriesMapper seriesMapper = mock(MriSeriesMapper.class);
        ImageFileMapper fileMapper = mock(ImageFileMapper.class);
        DownloadLogMapper downloadLogMapper = mock(DownloadLogMapper.class);
        when(studyMapper.deleteById((Serializable) 99L)).thenReturn(0);
        MybatisImageStudyRepository repository = new MybatisImageStudyRepository(studyMapper, seriesMapper, fileMapper, downloadLogMapper);

        assertThatThrownBy(() -> repository.deleteStudy(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Study 不存在");
    }
}
