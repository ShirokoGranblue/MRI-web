package com.mri.image.client;

import org.springframework.stereotype.Component;

@Component
public class RemoteExamClient implements ExamClient {
    private final ExamFeignApi api;

    public RemoteExamClient(ExamFeignApi api) {
        this.api = api;
    }

    @Override
    public boolean examExists(Long examOrderId) {
        try {
            return Boolean.TRUE.equals(api.exists(examOrderId).data().get("exists"));
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
