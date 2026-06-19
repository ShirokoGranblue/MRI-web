package com.mri.gateway.filter;

import com.mri.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAuthFilterTest {
    @Test
    void permitsLoginAndSwaggerWithoutToken() {
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        GatewayTokenValidator validator = new GatewayTokenValidator(jwt, token -> Mono.just(false));

        assertThat(validator.isPublicPath("/api/auth/login")).isTrue();
        assertThat(validator.isPublicPath("/swagger-ui/index.html")).isTrue();
        assertThat(validator.isPublicPath("/api/patients/1")).isFalse();
    }

    @Test
    void rejectsMissingTokenAndAcceptsValidBearerToken() {
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        GatewayTokenValidator validator = new GatewayTokenValidator(jwt, token -> Mono.just(false));
        String token = jwt.createToken("doctor", Set.of("RADIOLOGIST"));

        assertThat(validator.isAuthorized("/api/patients/1", null).block()).isFalse();
        assertThat(validator.isAuthorized("/api/patients/1", "bad-token").block()).isFalse();
        assertThat(validator.isAuthorized("/api/patients/1", "Bearer " + token).block()).isTrue();
    }

    @Test
    void rejectsBlacklistedToken() {
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        String token = jwt.createToken("doctor", Set.of("RADIOLOGIST"));
        GatewayTokenValidator validator = new GatewayTokenValidator(jwt, blacklisted -> Mono.just(blacklisted.equals(token)));

        assertThat(validator.isAuthorized("/api/reports", HttpHeaders.AUTHORIZATION + ": Bearer " + token).block()).isFalse();
        assertThat(validator.isAuthorized("/api/reports", "Bearer " + token).block()).isFalse();
    }

    @Test
    void enforcesAdminRoleForUserManagement() {
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        GatewayTokenValidator validator = new GatewayTokenValidator(jwt, token -> Mono.just(false));
        String doctorToken = jwt.createToken("doctor", Set.of("RADIOLOGIST"));
        String adminToken = jwt.createToken("admin", Set.of("ADMIN"));

        assertThat(validator.isAuthorized("/api/users", "Bearer " + doctorToken).block()).isFalse();
        assertThat(validator.isAuthorized("/api/users", "Bearer " + adminToken).block()).isTrue();
    }
}
