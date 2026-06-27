package com.mri.gateway.filter;

import com.mri.common.security.JwtClaims;

public record AuthorizationResult(AuthorizationStatus status, JwtClaims claims) {
    public static AuthorizationResult of(AuthorizationStatus status) {
        return new AuthorizationResult(status, null);
    }

    public static AuthorizationResult authorized(JwtClaims claims) {
        return new AuthorizationResult(AuthorizationStatus.AUTHORIZED, claims);
    }
}
