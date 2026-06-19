package com.mri.report.client;

import com.mri.common.api.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "mri-image-service", path = "/images")
public interface ImageFeignApi {
    @GetMapping("/studies/{id}")
    ApiResult<Map<String, Object>> study(@PathVariable("id") Long id);
}
