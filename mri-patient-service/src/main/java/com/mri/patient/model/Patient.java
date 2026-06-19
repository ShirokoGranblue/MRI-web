package com.mri.patient.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "患者档案")
public record Patient(
        @Schema(description = "患者ID") Long id,
        @Schema(description = "患者编号") String patientNo,
        @Schema(description = "姓名") String name,
        @Schema(description = "性别") String gender,
        @Schema(description = "出生日期") LocalDate birthDate,
        @Schema(description = "联系电话") String phone
) {
}
