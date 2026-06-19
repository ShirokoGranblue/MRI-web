package com.mri.exam.client;

import com.mri.common.api.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "mri-patient-service", path = "/patients")
public interface PatientFeignApi {
    @GetMapping("/{id}/exists")
    ApiResult<Map<String, Boolean>> exists(@PathVariable("id") Long id);
}
