package com.mri.patient.controller;

import com.mri.common.api.ApiResult;
import com.mri.common.api.PageResult;
import com.mri.patient.model.Contraindication;
import com.mri.patient.model.Patient;
import com.mri.patient.model.PatientExamHistory;
import com.mri.patient.repository.PatientRepository;
import com.mri.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "患者接口", description = "患者档案、MRI 禁忌症、患者检查历史")
@RestController
@RequestMapping("/patients")
public class PatientController {
    private final PatientService patientService;
    private final PatientRepository repository;

    public PatientController(PatientService patientService, PatientRepository repository) {
        this.patientService = patientService;
        this.repository = repository;
    }

    @Operation(summary = "新增患者")
    @PostMapping
    public ApiResult<Patient> create(@RequestBody Patient patient) {
        return ApiResult.ok(patientService.create(patient));
    }

    @Operation(summary = "删除患者")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ApiResult.ok();
    }

    @Operation(summary = "修改患者")
    @PutMapping("/{id}")
    public ApiResult<Patient> update(@PathVariable Long id, @RequestBody Patient patient) {
        return ApiResult.ok(patientService.update(new Patient(id, patient.patientNo(), patient.name(), patient.gender(), patient.birthDate(), patient.phone())));
    }

    @Operation(summary = "患者详情")
    @GetMapping("/{id}")
    public ApiResult<Patient> detail(@PathVariable Long id) {
        return ApiResult.ok(patientService.findById(id));
    }

    @Operation(summary = "患者分页查询")
    @GetMapping
    public ApiResult<PageResult<Patient>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword) {
        return ApiResult.ok(patientService.page(page, size, keyword));
    }

    @Operation(summary = "新增 MRI 禁忌症")
    @PostMapping("/{patientId}/contraindications")
    public ApiResult<Contraindication> createContraindication(@PathVariable Long patientId, @RequestBody Contraindication contraindication) {
        return ApiResult.ok(repository.createContraindication(new Contraindication(null, patientId, contraindication.type(), contraindication.description(), contraindication.severity())));
    }

    @Operation(summary = "删除 MRI 禁忌症")
    @DeleteMapping("/contraindications/{id}")
    public ApiResult<Void> deleteContraindication(@PathVariable Long id) {
        repository.deleteContraindication(id);
        return ApiResult.ok();
    }

    @Operation(summary = "修改 MRI 禁忌症")
    @PutMapping("/contraindications/{id}")
    public ApiResult<Contraindication> updateContraindication(@PathVariable Long id, @RequestBody Contraindication contraindication) {
        return ApiResult.ok(repository.updateContraindication(new Contraindication(id, contraindication.patientId(), contraindication.type(), contraindication.description(), contraindication.severity())));
    }

    @Operation(summary = "MRI 禁忌症详情")
    @GetMapping("/contraindications/{id}")
    public ApiResult<Contraindication> contraindicationDetail(@PathVariable Long id) {
        return ApiResult.ok(repository.findContraindication(id).orElseThrow(() -> new IllegalArgumentException("禁忌症不存在")));
    }

    @Operation(summary = "患者 MRI 禁忌症列表")
    @GetMapping("/{patientId}/contraindications")
    public ApiResult<List<Contraindication>> listContraindications(@PathVariable Long patientId) {
        return ApiResult.ok(repository.listContraindications(patientId));
    }

    @Operation(summary = "患者检查历史")
    @GetMapping("/{patientId}/exam-history")
    public ApiResult<List<PatientExamHistory>> examHistory(@PathVariable Long patientId) {
        return ApiResult.ok(repository.examHistory(patientId));
    }

    @Operation(summary = "患者存在性校验")
    @GetMapping("/{id}/exists")
    public ApiResult<Map<String, Boolean>> exists(@PathVariable Long id) {
        return ApiResult.ok(Map.of("exists", repository.findById(id).isPresent()));
    }
}
