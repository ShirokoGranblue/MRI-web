package com.mri.auth.service;

import com.mri.auth.dto.LoginRequest;
import com.mri.auth.dto.LoginResponse;
import com.mri.auth.model.UserRecord;
import com.mri.auth.repository.UserRepository;
import com.mri.common.security.JwtTokenProvider;
import com.mri.common.security.PasswordHasher;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthService {
    private final UserRepository users;
    private final TokenBlacklist blacklist;
    private final PasswordHasher passwordHasher;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository users, TokenBlacklist blacklist, PasswordHasher passwordHasher, JwtTokenProvider jwtTokenProvider) {
        this.users = users;
        this.blacklist = blacklist;
        this.passwordHasher = passwordHasher;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        UserRecord user = users.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!passwordHasher.matches(request.password(), user.passwordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtTokenProvider.createToken(user.username(), user.roles());
        return new LoginResponse(token, "Bearer", jwtTokenProvider.remainingTtl(token).toSeconds());
    }

    public void logout(String token) {
        Duration ttl = jwtTokenProvider.remainingTtl(token);
        blacklist.blacklist(token, ttl);
    }
}
