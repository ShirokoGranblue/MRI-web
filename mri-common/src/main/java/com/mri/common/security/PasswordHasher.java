package com.mri.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    public String hash(String rawPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + digest(salt, rawPassword);
    }

    public boolean matches(String rawPassword, String encoded) {
        if (rawPassword == null || encoded == null || !encoded.contains(":")) {
            return false;
        }
        String[] parts = encoded.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        return MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.UTF_8), digest(salt, rawPassword).getBytes(StandardCharsets.UTF_8));
    }

    private static String digest(byte[] salt, String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            digest.update(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (Exception ex) {
            throw new IllegalStateException("密码哈希失败", ex);
        }
    }
}
