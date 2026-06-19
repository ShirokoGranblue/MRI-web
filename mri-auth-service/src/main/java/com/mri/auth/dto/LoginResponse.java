package com.mri.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应")
public record LoginResponse(
        @Schema(description = "JWT 访问令牌") String token,
        @Schema(description = "令牌类型") String tokenType,
        @Schema(description = "过期秒数") long expiresInSeconds
) {
}
