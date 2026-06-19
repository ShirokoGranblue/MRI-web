package com.mri.auth.service;

import java.time.Duration;

public interface TokenBlacklist {
    void blacklist(String token, Duration ttl);

    boolean contains(String token);
}
