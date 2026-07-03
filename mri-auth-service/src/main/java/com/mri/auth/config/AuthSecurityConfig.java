package com.mri.auth.config;

import com.mri.common.security.JwtTokenProvider;
import com.mri.common.security.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AuthSecurityConfig {
    @Bean
    public PasswordHasher passwordHasher() {
        return new PasswordHasher();
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${mri.security.jwt-secret}") String secret,
            @Value("${mri.security.token-ttl-minutes:120}") long ttlMinutes
    ) {
        return new JwtTokenProvider(secret, Duration.ofMinutes(ttlMinutes));
    }
}
