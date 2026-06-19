package com.mri.gateway.filter;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RedisTokenBlacklistLookup implements TokenBlacklistLookup {
    private static final String PREFIX = "mri:token:blacklist:";

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisTokenBlacklistLookup(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Boolean> contains(String token) {
        return redisTemplate.hasKey(PREFIX + token).defaultIfEmpty(false);
    }
}
