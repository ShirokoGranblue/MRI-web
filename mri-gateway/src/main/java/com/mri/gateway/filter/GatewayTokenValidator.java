package com.mri.gateway.filter;

import com.mri.common.security.JwtClaims;
import com.mri.common.security.JwtTokenProvider;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

public class GatewayTokenValidator {
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/swagger-ui",
            "/webjars",
            "/v3/api-docs",
            "/actuator/health"
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistLookup blacklistLookup;

    public GatewayTokenValidator(JwtTokenProvider jwtTokenProvider, TokenBlacklistLookup blacklistLookup) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.blacklistLookup = blacklistLookup;
    }

    public boolean isPublicPath(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    public Mono<Boolean> isAuthorized(String path, String authorizationHeader) {
        if (isPublicPath(path)) {
            return Mono.just(true);
        }
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Mono.just(false);
        }
        String token = authorizationHeader.substring(7);
        if (!jwtTokenProvider.validate(token)) {
            return Mono.just(false);
        }
        JwtClaims claims = jwtTokenProvider.parse(token);
        if (!isRoleAllowed(path, claims.roles())) {
            return Mono.just(false);
        }
        return blacklistLookup.contains(token).map(blacklisted -> !blacklisted);
    }

    private boolean isRoleAllowed(String path, Set<String> roles) {
        if (path.startsWith("/api/users")) {
            return hasAnyRole(roles, "ADMIN");
        }
        if ((path.endsWith("/approve") || path.endsWith("/reject") || path.endsWith("/publish"))
                && path.startsWith("/api/reports/")) {
            return hasAnyRole(roles, "ADMIN", "AUDITOR");
        }
        return true;
    }

    private boolean hasAnyRole(Set<String> roles, String... allowedRoles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String allowedRole : allowedRoles) {
            if (roles.contains(allowedRole)) {
                return true;
            }
        }
        return false;
    }
}
