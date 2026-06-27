package com.mri.report.client;

import com.mri.common.api.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "mri-exam-service", path = "/exams")
public interface ExamFeignApi {
    @PostMapping("/{id}/reported")
    ApiResult<?> markReported(@PathVariable("id") Long id);

    @GetMapping("/{id}/status")
    ApiResult<Map<String, String>> status(@PathVariable("id") Long id);
}
