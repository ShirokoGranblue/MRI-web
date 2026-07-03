package com.mri.exam.service;

import com.mri.exam.dto.PatientExamView;
import com.mri.exam.mapper.PatientExamAccessMapper;
import com.mri.exam.repository.ExamOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientExamQueryService {
    private final PatientExamAccessMapper accessMapper;
    private final ExamOrderRepository repository;

    public PatientExamQueryService(PatientExamAccessMapper accessMapper, ExamOrderRepository repository) {
        this.accessMapper = accessMapper;
        this.repository = repository;
    }

    public Long requirePatientId(String username) {
        Long patientId = accessMapper.findPatientId(username);
        if (patientId == null) {
            throw new IllegalArgumentException("请先完成患者资料");
        }
        return patientId;
    }

    public List<PatientExamView> findMine(String username) {
        Long patientId = accessMapper.findPatientId(username);
        if (patientId == null) {
            return List.of();
        }
        return repository.listByPatient(patientId).stream()
                .map(exam -> PatientExamView.from(exam, repository.listSchedules(exam.id())))
                .toList();
    }
}
