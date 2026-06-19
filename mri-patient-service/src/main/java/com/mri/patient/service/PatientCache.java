package com.mri.patient.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mri.patient.model.Patient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PatientCache {
    private static final String KEY_PREFIX = "mri:patient:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final Map<Long, Patient> fallbackPatients = new ConcurrentHashMap<>();

    public PatientCache() {
        this(null, null, Duration.ofMinutes(5));
    }

    public PatientCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    public Optional<Patient> get(Long id) {
        if (redisTemplate == null) {
            return Optional.ofNullable(fallbackPatients.get(id));
        }
        String payload = redisTemplate.opsForValue().get(key(id));
        if (payload == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, Patient.class));
        } catch (JsonProcessingException ex) {
            evict(id);
            return Optional.empty();
        }
    }

    public void put(Patient patient) {
        if (redisTemplate == null) {
            fallbackPatients.put(patient.id(), patient);
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(patient.id()), objectMapper.writeValueAsString(patient), ttl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("患者缓存序列化失败", ex);
        }
    }

    public void evict(Long id) {
        if (redisTemplate == null) {
            fallbackPatients.remove(id);
            return;
        }
        redisTemplate.delete(key(id));
    }

    public boolean contains(Long id) {
        if (redisTemplate == null) {
            return fallbackPatients.containsKey(id);
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(id)));
    }

    private String key(Long id) {
        return KEY_PREFIX + id;
    }
}
