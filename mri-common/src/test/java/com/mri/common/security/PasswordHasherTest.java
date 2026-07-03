package com.mri.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashUsesPbkdf2Sha256() {
        String encoded = hasher.hash("admin123");

        assertTrue(encoded.startsWith("pbkdf2-sha256$210000$"));
        assertTrue(hasher.matches("admin123", encoded));
    }

    @Test
    void matchesLegacySha256HashDuringMigration() {
        String legacy = "bXJpLWRlbW8tc2FsdC0wMQ==:RXNbRsnC6O0uocAF8JkAe7ozzmURjU7gnQYBDpcs640=";

        assertTrue(hasher.matches("admin123", legacy));
    }

    @Test
    void rejectsPasswordsShorterThanEightCharacters() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash("1234567"));
    }
}
