package com.mri.auth.dto;

import com.mri.auth.model.UserRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "用户响应信息")
public record UserView(Long id, String username, String displayName, Set<String> roles) {
    public static UserView from(UserRecord user) {
        return new UserView(user.id(), user.username(), user.displayName(), user.roles());
    }
}
