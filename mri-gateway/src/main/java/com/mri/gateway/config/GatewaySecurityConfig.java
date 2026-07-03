package com.mri.gateway.config;

import com.mri.common.security.JwtTokenProvider;
import com.mri.gateway.filter.GatewayTokenValidator;
import com.mri.gateway.filter.TokenBlacklistLookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GatewaySecurityConfig {
    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${mri.security.jwt-secret}") String secret,
            @Value("${mri.security.token-ttl-minutes:120}") long ttlMinutes
    ) {
        return new JwtTokenProvider(secret, Duration.ofMinutes(ttlMinutes));
    }

    @Bean
    public GatewayTokenValidator gatewayTokenValidator(JwtTokenProvider jwtTokenProvider, TokenBlacklistLookup blacklistLookup) {
        return new GatewayTokenValidator(jwtTokenProvider, blacklistLookup);
    }
}
