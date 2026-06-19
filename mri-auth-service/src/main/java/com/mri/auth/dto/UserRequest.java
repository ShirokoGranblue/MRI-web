package com.mri.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "用户新增或修改请求")
public record UserRequest(
        @Schema(description = "登录用户名", example = "doctor01") String username,
        @Schema(description = "登录密码，服务端会保存为哈希", example = "doctor123") String password,
        @Schema(description = "显示名称", example = "诊断医生一") String displayName,
        @Schema(description = "角色编码", example = "[\"RADIOLOGIST\"]") Set<String> roles
) {
}
