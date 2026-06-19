package com.mri.auth.service;

import com.mri.auth.dto.LoginRequest;
import com.mri.auth.dto.LoginResponse;
import com.mri.auth.model.UserRecord;
import com.mri.auth.repository.UserRepository;
import com.mri.common.security.JwtTokenProvider;
import com.mri.common.security.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    @Test
    void loginReturnsTokenForValidPassword() {
        UserRepository users = mock(UserRepository.class);
        TokenBlacklist blacklist = mock(TokenBlacklist.class);
        PasswordHasher hasher = new PasswordHasher();
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        when(users.findByUsername("admin")).thenReturn(Optional.of(new UserRecord(1L, "admin", hasher.hash("admin123"), "管理员", Set.of("ADMIN"))));

        AuthService service = new AuthService(users, blacklist, hasher, jwt);
        LoginResponse response = service.login(new LoginRequest("admin", "admin123"));

        assertThat(response.token()).isNotBlank();
        assertThat(jwt.validate(response.token())).isTrue();
        assertThat(jwt.parse(response.token()).subject()).isEqualTo("admin");
    }

    @Test
    void loginRejectsWrongPassword() {
        UserRepository users = mock(UserRepository.class);
        TokenBlacklist blacklist = mock(TokenBlacklist.class);
        PasswordHasher hasher = new PasswordHasher();
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        when(users.findByUsername("admin")).thenReturn(Optional.of(new UserRecord(1L, "admin", hasher.hash("admin123"), "管理员", Set.of("ADMIN"))));

        AuthService service = new AuthService(users, blacklist, hasher, jwt);

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "bad")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void logoutBlacklistsTokenUntilTokenExpiry() {
        UserRepository users = mock(UserRepository.class);
        TokenBlacklist blacklist = mock(TokenBlacklist.class);
        PasswordHasher hasher = new PasswordHasher();
        JwtTokenProvider jwt = new JwtTokenProvider("0123456789abcdef0123456789abcdef", Duration.ofHours(2));
        String token = jwt.createToken("admin", Set.of("ADMIN"));

        AuthService service = new AuthService(users, blacklist, hasher, jwt);
        service.logout(token);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(blacklist).blacklist(org.mockito.ArgumentMatchers.eq(token), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isPositive();
        assertThat(ttlCaptor.getValue()).isLessThanOrEqualTo(Duration.ofHours(2));
    }
}
