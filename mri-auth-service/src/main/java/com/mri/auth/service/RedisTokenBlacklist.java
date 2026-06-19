package com.mri.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisTokenBlacklist implements TokenBlacklist {
    private static final String PREFIX = "mri:token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenBlacklist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklist(String token, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + token, "1", ttl);
    }

    @Override
    public boolean contains(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
