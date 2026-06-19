package com.mri.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class JwtTokenProvider {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final Duration ttl;

    public JwtTokenProvider(String secret, Duration ttl) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("JWT secret length must be at least 16 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttl = ttl;
    }

    public String createToken(String subject, Set<String> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(ttl);
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", subject);
        payload.put("roles", roles);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());
        String unsigned = encodeJson(header) + "." + encodeJson(payload);
        return unsigned + "." + sign(unsigned);
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public JwtClaims parse(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("无效 token");
        }
        String unsigned = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsigned), parts[2])) {
            throw new IllegalArgumentException("token 签名无效");
        }
        Map<String, Object> payload = decodeJson(parts[1]);
        Instant expiresAt = Instant.ofEpochSecond(((Number) payload.get("exp")).longValue());
        if (expiresAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("token 已过期");
        }
        Instant issuedAt = Instant.ofEpochSecond(((Number) payload.get("iat")).longValue());
        @SuppressWarnings("unchecked")
        Set<String> roles = Set.copyOf((Collection<String>) payload.get("roles"));
        return new JwtClaims(String.valueOf(payload.get("sub")), roles, issuedAt, expiresAt);
    }

    public Duration remainingTtl(String token) {
        JwtClaims claims = parse(token);
        long seconds = claims.expiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        return seconds <= 0 ? Duration.ZERO : Duration.ofSeconds(seconds);
    }

    private static String encodeJson(Object value) {
        try {
            return URL_ENCODER.encodeToString(OBJECT_MAPPER.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT JSON 序列化失败", ex);
        }
    }

    private static Map<String, Object> decodeJson(String encoded) {
        try {
            return OBJECT_MAPPER.readValue(URL_DECODER.decode(encoded), new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("JWT JSON 解析失败", ex);
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT 签名失败", ex);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
