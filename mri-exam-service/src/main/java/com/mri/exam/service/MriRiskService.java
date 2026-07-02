package com.mri.exam.service;

import com.mri.exam.model.PatientContraindication;
import com.mri.exam.model.RiskAssessment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MriRiskService {
    public RiskAssessment assess(List<PatientContraindication> contraindications) {
        List<PatientContraindication> items = contraindications == null ? List.of() : List.copyOf(contraindications);
        if (items.isEmpty()) {
            return new RiskAssessment("NONE", "未登记 MRI 禁忌症", LocalDateTime.now(), items);
        }

        boolean highRisk = items.stream().anyMatch(item -> !"LOW".equalsIgnoreCase(item.severity()));
        String summary = items.stream()
                .map(this::summaryItem)
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
        if (summary.length() > 1024) {
            summary = summary.substring(0, 1024);
        }
        return new RiskAssessment(highRisk ? "HIGH" : "LOW", summary, LocalDateTime.now(), items);
    }

    private String summaryItem(PatientContraindication item) {
        String type = textOrDefault(item.type(), "未命名禁忌症");
        String description = textOrDefault(item.description(), "未填写说明");
        String severity = "LOW".equalsIgnoreCase(item.severity()) ? "低风险" : "高风险";
        return type + "（" + severity + "：" + description + "）";
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
