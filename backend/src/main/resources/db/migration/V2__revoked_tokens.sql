-- 로그아웃 · 탈퇴로 무효화된 JWT 블랙리스트 (API 명세 POST /auth/logout).
-- 토큰 원문 대신 jti만 저장하고, 만료 시각이 지난 행은 주기적으로 삭제한다.
CREATE TABLE revoked_tokens (
    jti         varchar(36) PRIMARY KEY,
    expires_at  timestamp   NOT NULL
);
CREATE INDEX idx_revoked_tokens_expires ON revoked_tokens (expires_at);
