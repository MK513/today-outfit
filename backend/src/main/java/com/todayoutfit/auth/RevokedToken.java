package com.todayoutfit.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그아웃 · 탈퇴로 무효화된 토큰의 jti. 만료 시각 이후에는 검사할 필요가 없어 정리된다. */
@Getter
@Entity
@Table(name = "revoked_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevokedToken {

    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public RevokedToken(String jti, LocalDateTime expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }
}
