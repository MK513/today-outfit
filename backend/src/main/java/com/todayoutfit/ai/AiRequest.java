package com.todayoutfit.ai;

import com.todayoutfit.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** AI 호출 메타데이터(일일 한도 집계·실패 모니터링용). AI 결과 자체는 저장하지 않는다. */
@Getter
@Entity
@Table(name = "ai_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AiRequestStatus status;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AiRequest(User user, AiRequestType type, AiRequestStatus status, Integer latencyMs) {
        this.user = user;
        this.type = type;
        this.status = status;
        this.latencyMs = latencyMs;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
