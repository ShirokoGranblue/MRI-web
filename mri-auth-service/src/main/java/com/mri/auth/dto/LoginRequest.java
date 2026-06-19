package com.mri.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录请求")
public record LoginRequest(
        @Schema(description = "用户名", example = "admin") String username,
        @Schema(description = "密码", example = "admin123") String password
) {
}
