package com.mri.report.client;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RemoteImageClient implements ImageClient {
    private final ImageFeignApi api;

    public RemoteImageClient(ImageFeignApi api) {
        this.api = api;
    }

    @Override
    public String studyDescription(Long studyId) {
        Map<String, Object> data = api.study(studyId).data();
        Object description = data.get("description");
        return description == null ? "MRI Study" : String.valueOf(description);
    }

    @Override
    public Long studyExamOrderId(Long studyId) {
        try {
            Object value = api.study(studyId).data().get("examOrderId");
            if (value instanceof Number number) {
                return number.longValue();
            }
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
