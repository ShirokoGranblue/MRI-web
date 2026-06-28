package com.mri.gateway.filter;

import com.mri.common.security.JwtClaims;
import com.mri.common.security.JwtTokenProvider;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

public class GatewayTokenValidator {
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/login",
            "/api/auth/register",
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

    public Mono<AuthorizationResult> authorize(String path, HttpMethod method, String authorizationHeader) {
        if (isPublicPath(path)) {
            return Mono.just(AuthorizationResult.of(AuthorizationStatus.PUBLIC));
        }
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Mono.just(AuthorizationResult.of(AuthorizationStatus.UNAUTHENTICATED));
        }
        String token = authorizationHeader.substring(7);
        if (!jwtTokenProvider.validate(token)) {
            return Mono.just(AuthorizationResult.of(AuthorizationStatus.UNAUTHENTICATED));
        }
        JwtClaims claims = jwtTokenProvider.parse(token);
        if (!isRoleAllowed(path, method, claims.roles())) {
            return Mono.just(AuthorizationResult.of(AuthorizationStatus.FORBIDDEN));
        }
        return blacklistLookup.contains(token)
                .map(blacklisted -> blacklisted
                        ? AuthorizationResult.of(AuthorizationStatus.UNAUTHENTICATED)
                        : AuthorizationResult.authorized(claims));
    }

    private boolean isRoleAllowed(String path, HttpMethod method, Set<String> roles) {
        if (hasAnyRole(roles, "PATIENT")) {
            return isPatientPathAllowed(path, method);
        }
        if (path.startsWith("/api/users")) {
            return hasAnyRole(roles, "ADMIN");
        }
        if (path.startsWith("/api/patients") && method != HttpMethod.GET) {
            return false;
        }
        if ((path.endsWith("/approve") || path.endsWith("/reject") || path.endsWith("/publish"))
                && path.startsWith("/api/reports/")) {
            return hasAnyRole(roles, "ADMIN", "AUDITOR");
        }
        return true;
    }

    private boolean isPatientPathAllowed(String path, HttpMethod method) {
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        if (path.equals("/api/patients/me")) {
            return method == HttpMethod.GET || method == HttpMethod.POST || method == HttpMethod.PUT;
        }
        // 患者可提交本人检查申请
        if (path.equals("/api/exams") && method == HttpMethod.POST) {
            return true;
        }
        return method == HttpMethod.GET && (
                path.equals("/api/exams/mine")
                        || path.startsWith("/api/images/mine/")
                        || path.equals("/api/reports/mine")
        );
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
