package com.mri.auth.controller;

import com.mri.auth.dto.UserRequest;
import com.mri.auth.dto.UserView;
import com.mri.auth.model.UserRecord;
import com.mri.auth.repository.UserRepository;
import com.mri.common.api.ApiResult;
import com.mri.common.security.PasswordHasher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户接口", description = "系统用户新增、删除、修改、查询")
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository users;
    private final PasswordHasher passwordHasher;

    public UserController(UserRepository users, PasswordHasher passwordHasher) {
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public ApiResult<UserView> create(@RequestBody UserRequest user) {
        return ApiResult.ok(UserView.from(users.create(toRecord(null, user))));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        users.delete(id);
        return ApiResult.ok();
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public ApiResult<UserView> update(@PathVariable Long id, @RequestBody UserRequest user) {
        return ApiResult.ok(UserView.from(users.update(toRecord(id, user))));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{username}")
    public ApiResult<UserView> detail(@PathVariable String username) {
        return ApiResult.ok(UserView.from(users.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("用户不存在"))));
    }

    @Operation(summary = "用户列表")
    @GetMapping
    public ApiResult<List<UserView>> list() {
        return ApiResult.ok(users.list().stream().map(UserView::from).toList());
    }

    private UserRecord toRecord(Long id, UserRequest user) {
        if (user.password() == null || user.password().isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return new UserRecord(id, user.username(), passwordHasher.hash(user.password()), user.displayName(), user.roles());
    }
}
