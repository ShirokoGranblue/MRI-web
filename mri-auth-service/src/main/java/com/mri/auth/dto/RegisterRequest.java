package com.mri.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "患者注册请求")
public record RegisterRequest(
        @Schema(description = "患者姓名", example = "张三") String displayName,
        @Schema(description = "登录用户名", example = "patient01") String username,
        @Schema(description = "登录密码", example = "patient123") String password
) {
}
