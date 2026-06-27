package com.mri.auth.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mri.auth.entity.UserEntity;
import com.mri.auth.entity.UserRoleEntity;
import com.mri.auth.mapper.UserMapper;
import com.mri.auth.mapper.UserRoleMapper;
import com.mri.auth.model.UserRecord;
import com.mri.common.exception.ConflictException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class MybatisUserRepository implements UserRepository {
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public MybatisUserRepository(UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public Optional<UserRecord> findByUsername(String username) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getEnabled, "Y"));
        return Optional.ofNullable(user).map(this::toRecord);
    }

    @Override
    public List<UserRecord> list() {
        return userMapper.selectList(new LambdaQueryWrapper<>()).stream().map(this::toRecord).toList();
    }

    @Override
    public UserRecord create(UserRecord user) {
        UserEntity entity = toEntity(user);
        try {
            userMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("该用户名已被使用，请更换用户名");
        }
        replaceRoles(entity.getId(), user.roles());
        return toRecord(entity);
    }

    @Override
    public UserRecord update(UserRecord user) {
        if (userMapper.selectById(user.id()) == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        UserEntity entity = toEntity(user);
        ensureAffected(userMapper.updateById(entity), "用户不存在");
        replaceRoles(user.id(), user.roles());
        return user;
    }

    @Override
    public void delete(Long id) {
        ensureAffected(userMapper.deleteById(id), "用户不存在");
    }

    private UserRecord toRecord(UserEntity entity) {
        List<UserRoleEntity> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, entity.getId()));
        Set<String> roleCodes = roles.stream().map(UserRoleEntity::getRoleCode).collect(Collectors.toSet());
        return new UserRecord(entity.getId(), entity.getUsername(), entity.getPasswordHash(), entity.getDisplayName(), roleCodes);
    }

    private static UserEntity toEntity(UserRecord record) {
        UserEntity entity = new UserEntity();
        entity.setId(record.id());
        entity.setUsername(record.username());
        entity.setPasswordHash(record.passwordHash());
        entity.setDisplayName(record.displayName());
        entity.setEnabled("Y");
        return entity;
    }

    private void replaceRoles(Long userId, Set<String> roles) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId));
        if (roles == null) {
            return;
        }
        for (String role : roles) {
            UserRoleEntity entity = new UserRoleEntity();
            entity.setUserId(userId);
            entity.setRoleCode(role);
            userRoleMapper.insert(entity);
        }
    }

    private static void ensureAffected(int affectedRows, String message) {
        if (affectedRows <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
