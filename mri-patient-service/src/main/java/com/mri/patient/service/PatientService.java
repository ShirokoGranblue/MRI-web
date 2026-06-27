package com.mri.patient.service;

import com.mri.common.api.PageResult;
import com.mri.common.exception.ConflictException;
import com.mri.patient.client.ExamClient;
import com.mri.patient.dto.PatientProfileRequest;
import com.mri.patient.dto.PatientProfileView;
import com.mri.patient.model.Contraindication;
import com.mri.patient.model.Patient;
import com.mri.patient.model.PatientExamHistory;
import com.mri.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PatientService {
    private final PatientRepository repository;
    private final PatientCache cache;
    private final ExamClient examClient;

    public PatientService(PatientRepository repository, PatientCache cache, ExamClient examClient) {
        this.repository = repository;
        this.cache = cache;
        this.examClient = examClient;
    }

    public Patient create(Patient patient) {
        Patient created = repository.create(patient);
        cache.evict(created.id());
        return created;
    }

    public Patient findById(Long id) {
        return cache.get(id).orElseGet(() -> {
            Patient patient = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("患者不存在"));
            cache.put(patient);
            return patient;
        });
    }

    public PageResult<Patient> page(long page, long size, String keyword) {
        return repository.page(page, size, keyword);
    }

    public Patient update(Patient patient) {
        Patient updated = repository.update(patient);
        cache.evict(patient.id());
        return updated;
    }

    public void delete(Long id) {
        repository.delete(id);
        cache.evict(id);
    }

    public List<PatientExamHistory> examHistory(Long patientId) {
        return examClient.listByPatient(patientId).stream()
                .map(e -> new PatientExamHistory(e.patientId(), e.examItem(), e.status(), e.createdAt()))
                .toList();
    }

    public PatientProfileView profileFor(String username) {
        Optional<Patient> patient = repository.findByAccountUsername(requireUsername(username));
        if (patient.isEmpty()) {
            return PatientProfileView.incomplete();
        }
        List<Contraindication> contraindications = repository.listContraindications(patient.get().id());
        return new PatientProfileView(true, patient.get(), !contraindications.isEmpty(), contraindications);
    }

    @Transactional
    public PatientProfileView createProfile(String username, PatientProfileRequest request) {
        String accountUsername = requireUsername(username);
        validateProfile(request);
        if (repository.findByAccountUsername(accountUsername).isPresent()) {
            throw new ConflictException("患者档案已存在，请使用修改功能");
        }
        Patient created = repository.createForAccount(toPatient(null, generatePatientNo(), request), accountUsername);
        repository.replaceContraindications(created.id(), toContraindications(created.id(), request));
        cache.evict(created.id());
        return profileFor(accountUsername);
    }

    @Transactional
    public PatientProfileView updateProfile(String username, PatientProfileRequest request) {
        String accountUsername = requireUsername(username);
        validateProfile(request);
        Patient existing = repository.findByAccountUsername(accountUsername)
                .orElseThrow(() -> new IllegalArgumentException("请先完成患者资料"));
        Patient updated = repository.updateForAccount(toPatient(existing.id(), existing.patientNo(), request), accountUsername);
        repository.replaceContraindications(updated.id(), toContraindications(updated.id(), request));
        cache.evict(updated.id());
        return profileFor(accountUsername);
    }

    private static Patient toPatient(Long id, String patientNo, PatientProfileRequest request) {
        return new Patient(id, patientNo, request.name().trim(), request.gender(), request.birthDate(), trimToNull(request.phone()));
    }

    private static List<Contraindication> toContraindications(Long patientId, PatientProfileRequest request) {
        if (!request.hasContraindications()) {
            return List.of();
        }
        return request.contraindications().stream()
                .map(input -> new Contraindication(
                        null,
                        patientId,
                        input.type().trim(),
                        trimToNull(input.description()),
                        input.severity()
                ))
                .toList();
    }

    private static void validateProfile(PatientProfileRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (request.gender() == null || request.gender().isBlank()) {
            throw new IllegalArgumentException("性别不能为空");
        }
        List<PatientProfileRequest.ContraindicationInput> inputs =
                request.contraindications() == null ? List.of() : request.contraindications();
        if (request.hasContraindications() && inputs.isEmpty()) {
            throw new IllegalArgumentException("请至少登记一条禁忌症");
        }
        if (!request.hasContraindications() && !inputs.isEmpty()) {
            throw new IllegalArgumentException("选择无禁忌症时不能提交禁忌症记录");
        }
        if (inputs.stream().anyMatch(input -> input.type() == null || input.type().isBlank())) {
            throw new IllegalArgumentException("禁忌症类型不能为空");
        }
    }

    private static String generatePatientNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "P" + date + suffix;
    }

    private static String requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("当前用户身份无效");
        }
        return username.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
