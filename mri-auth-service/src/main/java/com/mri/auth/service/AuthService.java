package com.mri.auth.service;

import com.mri.auth.dto.LoginRequest;
import com.mri.auth.dto.LoginResponse;
import com.mri.auth.dto.RegisterRequest;
import com.mri.auth.dto.RegisterResponse;
import com.mri.auth.model.UserRecord;
import com.mri.auth.repository.UserRepository;
import com.mri.common.exception.ConflictException;
import com.mri.common.security.JwtTokenProvider;
import com.mri.common.security.PasswordHasher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

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
        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenProvider.remainingTtl(token).toSeconds(),
                user.username(),
                user.displayName(),
                user.roles()
        );
    }

    public RegisterResponse register(RegisterRequest request) {
        String displayName = requireText(request.displayName(), "姓名不能为空");
        String username = requireText(request.username(), "用户名不能为空");
        String password = requireText(request.password(), "密码不能为空");
        if (users.findByUsername(username).isPresent()) {
            throw new ConflictException("该用户名已被使用，请更换用户名");
        }
        UserRecord created = users.create(new UserRecord(
                null,
                username,
                passwordHasher.hash(password),
                displayName,
                Set.of("PATIENT")
        ));
        return RegisterResponse.from(created);
    }

    public RegisterResponse currentUser(String username) {
        return users.findByUsername(requireText(username, "当前用户身份无效"))
                .map(RegisterResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在或已停用"));
    }

    public void logout(String token) {
        Duration ttl = jwtTokenProvider.remainingTtl(token);
        blacklist.blacklist(token, ttl);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
