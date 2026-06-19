package com.mri.report.client;

import org.springframework.stereotype.Component;

@Component
public class RemoteExamClient implements ExamClient {
    private final ExamFeignApi api;

    public RemoteExamClient(ExamFeignApi api) {
        this.api = api;
    }

    @Override
    public void markReported(Long examOrderId) {
        api.markReported(examOrderId);
    }
}
