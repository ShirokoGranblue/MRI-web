package com.mri.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mri.common.domain.BaseEntity;

@TableName("sys_user_role")
public class UserRoleEntity extends BaseEntity {
    private Long userId;
    private String roleCode;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}
