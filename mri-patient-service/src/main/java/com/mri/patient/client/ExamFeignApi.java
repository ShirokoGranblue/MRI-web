package com.mri.patient.client;

import com.mri.common.api.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "mri-exam-service", path = "/exams")
public interface ExamFeignApi {
    @GetMapping("/by-patient/{patientId}")
    ApiResult<List<ExamSummary>> byPatient(@PathVariable("patientId") Long patientId);
}
