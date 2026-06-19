package com.mri.image.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mri.image.model.MriStudy;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class StudyCache {
    private static final String STUDY_KEY_PREFIX = "mri:study:";
    private static final String VIEWER_KEY_PREFIX = "mri:viewer:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final Map<Long, MriStudy> fallbackStudies = new ConcurrentHashMap<>();
    private final Map<Long, ViewerManifest> fallbackManifests = new ConcurrentHashMap<>();

    public StudyCache() {
        this(null, null, Duration.ofMinutes(5));
    }

    public StudyCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    public Optional<MriStudy> getStudy(Long id) {
        if (redisTemplate == null) {
            return Optional.ofNullable(fallbackStudies.get(id));
        }
        return read(studyKey(id), MriStudy.class);
    }

    public void putStudy(MriStudy study) {
        if (redisTemplate == null) {
            fallbackStudies.put(study.id(), study);
            return;
        }
        write(studyKey(study.id()), study);
    }

    public Optional<ViewerManifest> getManifest(Long studyId, String watermark, boolean downloadEnabled) {
        Optional<ViewerManifest> cached = redisTemplate == null
                ? Optional.ofNullable(fallbackManifests.get(studyId))
                : read(viewerKey(studyId), ViewerManifest.class);
        if (cached.isEmpty()) {
            return Optional.empty();
        }
        ViewerManifest manifest = cached.get();
        if (!manifest.watermark().equals(watermark) || manifest.downloadEnabled() != downloadEnabled) {
            evict(studyId);
            return Optional.empty();
        }
        return cached;
    }

    public void putManifest(Long studyId, ViewerManifest manifest) {
        if (redisTemplate == null) {
            fallbackManifests.put(studyId, manifest);
            return;
        }
        write(viewerKey(studyId), manifest);
    }

    public void evict(Long studyId) {
        if (redisTemplate == null) {
            fallbackStudies.remove(studyId);
            fallbackManifests.remove(studyId);
            return;
        }
        redisTemplate.delete(studyKey(studyId));
        redisTemplate.delete(viewerKey(studyId));
    }

    public boolean containsStudy(Long studyId) {
        if (redisTemplate == null) {
            return fallbackStudies.containsKey(studyId);
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(studyKey(studyId)));
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, type));
        } catch (JsonProcessingException ex) {
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    private void write(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Study 缓存序列化失败", ex);
        }
    }

    private String studyKey(Long id) {
        return STUDY_KEY_PREFIX + id;
    }

    private String viewerKey(Long id) {
        return VIEWER_KEY_PREFIX + id;
    }
}
