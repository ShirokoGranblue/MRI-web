package com.mri.gateway.filter;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface TokenBlacklistLookup {
    Mono<Boolean> contains(String token);
}
