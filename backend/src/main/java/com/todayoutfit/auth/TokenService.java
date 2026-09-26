package com.todayoutfit.auth;

import com.todayoutfit.user.User;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Access Token 발급과 무효화(블랙리스트). subject는 사용자 ID, jti는 무효화 식별자다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final RevokedTokenRepository revokedTokenRepository;

    public AuthTokenResponse issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(properties.expiresIn()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthTokenResponse(token, "Bearer", properties.expiresIn(), UserResponse.from(user));
    }

    public void revoke(Jwt jwt) {
        if (jwt.getId() == null || jwt.getExpiresAt() == null) {
            return;
        }
        LocalDateTime expiresAt = LocalDateTime.ofInstant(jwt.getExpiresAt(), ZoneId.systemDefault());
        revokedTokenRepository.save(new RevokedToken(jwt.getId(), expiresAt));
    }

    public boolean isRevoked(String jti) {
        return jti != null && revokedTokenRepository.existsById(jti);
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1M")
    public void purgeExpired() {
        int deleted = revokedTokenRepository.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Purged {} expired revoked tokens", deleted);
        }
    }
}
