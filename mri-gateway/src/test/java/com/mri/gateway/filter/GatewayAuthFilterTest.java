package com.mri.gateway.filter;

import com.mri.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
        assertThat(validator.isPublicPath("/api/auth/register")).isTrue();
        assertThat(validator.isPublicPath("/swagger-ui/index.html")).isTrue();
        assertThat(validator.isPublicPath("/api/patients/1")).isFalse();
    }

    @Test
    void rejectsMissingTokenAndAcceptsValidBearerToken() {
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        GatewayTokenValidator validator = new GatewayTokenValidator(jwt, token -> Mono.just(false));
        String token = jwt.createToken("doctor", Set.of("RADIOLOGIST"));

        assertThat(validator.authorize("/api/patients/1", HttpMethod.GET, null).block().status())
                .isEqualTo(AuthorizationStatus.UNAUTHENTICATED);
        assertThat(validator.authorize("/api/patients/1", HttpMethod.GET, "bad-token").block().status())
                .isEqualTo(AuthorizationStatus.UNAUTHENTICATED);
        assertThat(validator.authorize("/api/patients/1", HttpMethod.GET, "Bearer " + token).block().status())
                .isEqualTo(AuthorizationStatus.AUTHORIZED);
    }

    @Test
    void rejectsBlacklistedToken() {
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        String token = jwt.createToken("doctor", Set.of("RADIOLOGIST"));
        GatewayTokenValidator validator = new GatewayTokenValidator(jwt, blacklisted -> Mono.just(blacklisted.equals(token)));

        assertThat(validator.authorize("/api/reports", HttpMethod.GET, HttpHeaders.AUTHORIZATION + ": Bearer " + token).block().status())
                .isEqualTo(AuthorizationStatus.UNAUTHENTICATED);
        assertThat(validator.authorize("/api/reports", HttpMethod.GET, "Bearer " + token).block().status())
                .isEqualTo(AuthorizationStatus.UNAUTHENTICATED);
    }

    @Test
    void enforcesAdminRoleForUserManagement() {
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        GatewayTokenValidator validator = new GatewayTokenValidator(jwt, token -> Mono.just(false));
        String doctorToken = jwt.createToken("doctor", Set.of("RADIOLOGIST"));
        String adminToken = jwt.createToken("admin", Set.of("ADMIN"));

        assertThat(validator.authorize("/api/users", HttpMethod.GET, "Bearer " + doctorToken).block().status())
                .isEqualTo(AuthorizationStatus.FORBIDDEN);
        assertThat(validator.authorize("/api/users", HttpMethod.GET, "Bearer " + adminToken).block().status())
                .isEqualTo(AuthorizationStatus.AUTHORIZED);
    }

    @Test
    void patientCanOnlyUseSelfServiceEndpoints() {
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        GatewayTokenValidator validator = new GatewayTokenValidator(jwt, token -> Mono.just(false));
        String patientToken = jwt.createToken("patient01", Set.of("PATIENT"));

        assertThat(validator.authorize("/api/patients/me", HttpMethod.POST, "Bearer " + patientToken).block().status())
                .isEqualTo(AuthorizationStatus.AUTHORIZED);
        assertThat(validator.authorize("/api/exams/mine", HttpMethod.GET, "Bearer " + patientToken).block().status())
                .isEqualTo(AuthorizationStatus.AUTHORIZED);
        assertThat(validator.authorize("/api/images/mine/studies", HttpMethod.GET, "Bearer " + patientToken).block().status())
                .isEqualTo(AuthorizationStatus.AUTHORIZED);
        assertThat(validator.authorize("/api/reports/mine", HttpMethod.GET, "Bearer " + patientToken).block().status())
                .isEqualTo(AuthorizationStatus.AUTHORIZED);
        assertThat(validator.authorize("/api/exams", HttpMethod.POST, "Bearer " + patientToken).block().status())
                .isEqualTo(AuthorizationStatus.AUTHORIZED);
        assertThat(validator.authorize("/api/patients", HttpMethod.GET, "Bearer " + patientToken).block().status())
                .isEqualTo(AuthorizationStatus.FORBIDDEN);
        assertThat(validator.authorize("/api/exams/1/start", HttpMethod.POST, "Bearer " + patientToken).block().status())
                .isEqualTo(AuthorizationStatus.FORBIDDEN);
    }

    @Test
    void adminCanReadButCannotWritePatientProfiles() {
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        GatewayTokenValidator validator = new GatewayTokenValidator(jwt, token -> Mono.just(false));
        String adminToken = jwt.createToken("admin", Set.of("ADMIN", "RADIOLOGIST", "AUDITOR"));

        assertThat(validator.authorize("/api/patients", HttpMethod.GET, "Bearer " + adminToken).block().status())
                .isEqualTo(AuthorizationStatus.AUTHORIZED);
        assertThat(validator.authorize("/api/patients", HttpMethod.POST, "Bearer " + adminToken).block().status())
                .isEqualTo(AuthorizationStatus.FORBIDDEN);
        assertThat(validator.authorize("/api/patients/1", HttpMethod.PUT, "Bearer " + adminToken).block().status())
                .isEqualTo(AuthorizationStatus.FORBIDDEN);
        assertThat(validator.authorize("/api/patients/contraindications/1", HttpMethod.DELETE, "Bearer " + adminToken).block().status())
                .isEqualTo(AuthorizationStatus.FORBIDDEN);
    }
}
