package com.mri.patient.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mri.common.api.PageResult;
import com.mri.patient.entity.ContraindicationEntity;
import com.mri.patient.entity.PatientEntity;
import com.mri.patient.mapper.ContraindicationMapper;
import com.mri.patient.mapper.PatientMapper;
import com.mri.patient.model.Contraindication;
import com.mri.patient.model.Patient;
import com.mri.common.exception.ConflictException;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisPatientRepository implements PatientRepository {
    private final PatientMapper patientMapper;
    private final ContraindicationMapper contraindicationMapper;

    public MybatisPatientRepository(PatientMapper patientMapper, ContraindicationMapper contraindicationMapper) {
        this.patientMapper = patientMapper;
        this.contraindicationMapper = contraindicationMapper;
    }

    @Override
    public Patient create(Patient patient) {
        PatientEntity entity = toEntity(patient);
        patientMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public Patient createForAccount(Patient patient, String accountUsername) {
        PatientEntity entity = toEntity(patient);
        entity.setAccountUsername(accountUsername);
        try {
            patientMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("患者档案已存在，请使用修改功能");
        }
        return toModel(entity);
    }

    @Override
    public Optional<Patient> findById(Long id) {
        return Optional.ofNullable(patientMapper.selectById(id)).map(MybatisPatientRepository::toModel);
    }

    @Override
    public Optional<Patient> findByAccountUsername(String accountUsername) {
        return Optional.ofNullable(patientMapper.selectOne(new LambdaQueryWrapper<PatientEntity>()
                        .eq(PatientEntity::getAccountUsername, accountUsername)))
                .map(MybatisPatientRepository::toModel);
    }

    @Override
    public PageResult<Patient> page(long page, long size, String keyword) {
        LambdaQueryWrapper<PatientEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(PatientEntity::getName, keyword).or().like(PatientEntity::getPatientNo, keyword);
        }
        Page<PatientEntity> result = patientMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(page, size, result.getTotal(), result.getRecords().stream().map(MybatisPatientRepository::toModel).toList());
    }

    @Override
    public Patient update(Patient patient) {
        ensureAffected(patientMapper.updateById(toEntity(patient)), "患者不存在");
        return patient;
    }

    @Override
    public Patient updateForAccount(Patient patient, String accountUsername) {
        PatientEntity entity = toEntity(patient);
        entity.setAccountUsername(accountUsername);
        ensureAffected(patientMapper.updateById(entity), "患者不存在");
        return patient;
    }

    @Override
    public void delete(Long id) {
        ensureAffected(patientMapper.deleteById(id), "患者不存在");
    }

    @Override
    public Contraindication createContraindication(Contraindication contraindication) {
        ContraindicationEntity entity = toEntity(contraindication);
        contraindicationMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public Contraindication updateContraindication(Contraindication contraindication) {
        ensureAffected(contraindicationMapper.updateById(toEntity(contraindication)), "禁忌症不存在");
        return contraindication;
    }

    @Override
    public Optional<Contraindication> findContraindication(Long id) {
        return Optional.ofNullable(contraindicationMapper.selectById(id)).map(MybatisPatientRepository::toModel);
    }

    @Override
    public List<Contraindication> listContraindications(Long patientId) {
        return contraindicationMapper.selectList(new LambdaQueryWrapper<ContraindicationEntity>().eq(ContraindicationEntity::getPatientId, patientId))
                .stream().map(MybatisPatientRepository::toModel).toList();
    }

    @Override
    public void deleteContraindication(Long id) {
        ensureAffected(contraindicationMapper.deleteById(id), "禁忌症不存在");
    }

    @Override
    public void replaceContraindications(Long patientId, List<Contraindication> contraindications) {
        contraindicationMapper.delete(new LambdaQueryWrapper<ContraindicationEntity>()
                .eq(ContraindicationEntity::getPatientId, patientId));
        for (Contraindication contraindication : contraindications) {
            createContraindication(new Contraindication(
                    null,
                    patientId,
                    contraindication.type(),
                    contraindication.description(),
                    contraindication.severity()
            ));
        }
    }

    private static Patient toModel(PatientEntity entity) {
        return new Patient(entity.getId(), entity.getPatientNo(), entity.getName(), entity.getGender(), entity.getBirthDate(), entity.getPhone());
    }

    private static PatientEntity toEntity(Patient patient) {
        PatientEntity entity = new PatientEntity();
        entity.setId(patient.id());
        entity.setPatientNo(patient.patientNo());
        entity.setName(patient.name());
        entity.setGender(patient.gender());
        entity.setBirthDate(patient.birthDate());
        entity.setPhone(patient.phone());
        return entity;
    }

    private static Contraindication toModel(ContraindicationEntity entity) {
        return new Contraindication(entity.getId(), entity.getPatientId(), entity.getType(), entity.getDescription(), entity.getSeverity());
    }

    private static ContraindicationEntity toEntity(Contraindication contraindication) {
        ContraindicationEntity entity = new ContraindicationEntity();
        entity.setId(contraindication.id());
        entity.setPatientId(contraindication.patientId());
        entity.setType(contraindication.type());
        entity.setDescription(contraindication.description());
        entity.setSeverity(contraindication.severity());
        return entity;
    }

    private static void ensureAffected(int affectedRows, String message) {
        if (affectedRows <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
