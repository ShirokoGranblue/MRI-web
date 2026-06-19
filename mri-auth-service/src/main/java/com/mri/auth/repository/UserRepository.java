package com.mri.auth.repository;

import com.mri.auth.model.UserRecord;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<UserRecord> findByUsername(String username);

    List<UserRecord> list();

    UserRecord create(UserRecord user);

    UserRecord update(UserRecord user);

    void delete(Long id);
}
