package com.mri.auth.dto;

import com.mri.auth.model.UserRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "注册用户身份")
public record RegisterResponse(
        String username,
        String displayName,
        Set<String> roles
) {
    public static RegisterResponse from(UserRecord user) {
        return new RegisterResponse(user.username(), user.displayName(), user.roles());
    }
}
