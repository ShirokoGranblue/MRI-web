package com.mri.exam.service;

import com.mri.exam.model.PatientContraindication;
import com.mri.exam.model.RiskAssessment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MriRiskServiceTest {
    private final MriRiskService service = new MriRiskService();

    @Test
    void noContraindicationsMeansNoRisk() {
        RiskAssessment result = service.assess(List.of());

        assertThat(result.level()).isEqualTo("NONE");
        assertThat(result.summary()).isEqualTo("未登记 MRI 禁忌症");
        assertThat(result.evaluatedAt()).isNotNull();
    }

    @Test
    void allLowContraindicationsMeanLowRisk() {
        RiskAssessment result = service.assess(List.of(
                new PatientContraindication(1L, 3L, "幽闭恐惧", "轻度，可配合", "LOW"),
                new PatientContraindication(2L, 3L, "纹身", "已确认无金属成分", "LOW")
        ));

        assertThat(result.level()).isEqualTo("LOW");
        assertThat(result.summary()).contains("幽闭恐惧").contains("纹身");
        assertThat(result.items()).hasSize(2);
    }

    @Test
    void anyHighContraindicationMeansHighRisk() {
        RiskAssessment result = service.assess(List.of(
                new PatientContraindication(1L, 3L, "幽闭恐惧", "轻度", "LOW"),
                new PatientContraindication(2L, 3L, "心脏起搏器", "型号待核实", "HIGH")
        ));

        assertThat(result.level()).isEqualTo("HIGH");
    }

    @Test
    void missingOrUnknownSeverityIsHandledAsHighRisk() {
        assertThat(service.assess(List.of(
                new PatientContraindication(1L, 3L, "金属植入物", "材质不明", null)
        )).level()).isEqualTo("HIGH");
        assertThat(service.assess(List.of(
                new PatientContraindication(2L, 3L, "其他植入物", "风险未分级", "UNKNOWN")
        )).level()).isEqualTo("HIGH");
    }
}
