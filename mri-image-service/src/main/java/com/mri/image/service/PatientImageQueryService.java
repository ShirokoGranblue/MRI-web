package com.mri.image.service;

import com.mri.common.exception.ForbiddenException;
import com.mri.image.dto.PatientStudyView;
import com.mri.image.mapper.PatientImageAccessMapper;
import com.mri.image.repository.ImageStudyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientImageQueryService {
    private final PatientImageAccessMapper accessMapper;
    private final ImageStudyRepository repository;

    public PatientImageQueryService(PatientImageAccessMapper accessMapper, ImageStudyRepository repository) {
        this.accessMapper = accessMapper;
        this.repository = repository;
    }

    public List<PatientStudyView> findMine(String username) {
        return accessMapper.findStudyIdsByUsername(username).stream()
                .map(id -> repository.findStudyById(id)
                        .map(study -> new PatientStudyView(
                                study,
                                repository.findFilesByStudyId(id).size(),
                                accessMapper.isReportPublished(id, username)
                        ))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public void assertStudyVisible(Long studyId, String username) {
        if (!accessMapper.ownsStudy(studyId, username)) {
            throw new ForbiddenException("您无权查看该影像");
        }
        if (!accessMapper.isReportPublished(studyId, username)) {
            throw new ForbiddenException("诊断报告发布后方可查看影像");
        }
    }

    public void assertFileVisible(Long fileId, String username) {
        Long studyId = accessMapper.findStudyIdByFile(fileId, username);
        if (studyId == null) {
            throw new ForbiddenException("您无权查看该影像");
        }
        assertStudyVisible(studyId, username);
    }
}
