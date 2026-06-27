package com.mri.report.service;

import com.mri.report.dto.PatientReportView;
import com.mri.report.mapper.PatientReportAccessMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientReportQueryService {
    private final PatientReportAccessMapper accessMapper;

    public PatientReportQueryService(PatientReportAccessMapper accessMapper) {
        this.accessMapper = accessMapper;
    }

    public List<PatientReportView> findMine(String username) {
        return accessMapper.findReportsByUsername(username).stream()
                .map(PatientReportView::from)
                .toList();
    }
}
