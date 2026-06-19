package com.mri.patient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mri.patient.service.PatientCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
public class PatientCacheConfig {
    @Bean
    public PatientCache patientCache(StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper,
                                     @Value("${mri.cache.patient-ttl-seconds:300}") long ttlSeconds) {
        return new PatientCache(redisTemplate, objectMapper, Duration.ofSeconds(ttlSeconds));
    }
}
