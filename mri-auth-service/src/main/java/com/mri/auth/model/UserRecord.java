package com.mri.auth.model;

import java.util.Set;

public record UserRecord(Long id, String username, String passwordHash, String displayName, Set<String> roles) {
}
