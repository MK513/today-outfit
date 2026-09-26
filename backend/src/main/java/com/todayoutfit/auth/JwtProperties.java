package com.todayoutfit.auth;

import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret    HS256 서명 키 (Base64, 디코딩 후 32바이트 이상)
 * @param expiresIn Access Token 유효 시간(초)
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, long expiresIn) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set. See backend/.env.example");
        }
        if (Base64.getDecoder().decode(secret).length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
        }
    }

    public byte[] secretBytes() {
        return Base64.getDecoder().decode(secret);
    }
}
