package com.mri.auth.controller;

import com.mri.auth.dto.LoginRequest;
import com.mri.auth.dto.LoginResponse;
import com.mri.auth.dto.RegisterRequest;
import com.mri.auth.dto.RegisterResponse;
import com.mri.auth.service.AuthService;
import com.mri.common.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "认证接口", description = "登录、登出、刷新 token、当前用户")
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResult.ok(authService.login(request));
    }

    @Operation(summary = "患者注册")
    @PostMapping("/register")
    public ApiResult<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return ApiResult.ok(authService.register(request));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(stripBearer(authorization));
        return ApiResult.ok();
    }

    @Operation(summary = "刷新 token 演示")
    @PostMapping("/refresh")
    public ApiResult<Map<String, String>> refresh() {
        return ApiResult.ok(Map.of("message", "请重新登录获取新 token"));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public ApiResult<RegisterResponse> me(@RequestHeader("X-Authenticated-User") String username) {
        return ApiResult.ok(authService.currentUser(username));
    }

    private static String stripBearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
    }
}
