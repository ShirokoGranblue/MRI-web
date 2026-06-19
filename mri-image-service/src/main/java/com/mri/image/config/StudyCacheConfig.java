package com.mri.image.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mri.image.service.StudyCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
public class StudyCacheConfig {
    @Bean
    public StudyCache studyCache(StringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper,
                                 @Value("${mri.cache.study-ttl-seconds:300}") long ttlSeconds) {
        return new StudyCache(redisTemplate, objectMapper, Duration.ofSeconds(ttlSeconds));
    }
}
