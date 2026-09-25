package com.todayoutfit.outfit;

import com.todayoutfit.common.BaseTimeEntity;
import com.todayoutfit.user.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "outfits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outfit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OutfitSource source;

    /** source=AI 전용: 사용자가 입력한 상황 */
    @Column(name = "request_text", length = 500)
    private String requestText;

    /** source=AI 전용 */
    @Column(name = "ai_reason", columnDefinition = "text")
    private String aiReason;

    /** source=CHALLENGE 전용, 0~100. 평가 실패 시 NULL */
    @Column(name = "ai_score")
    private Integer aiScore;

    /** source=CHALLENGE 전용 */
    @Column(name = "ai_comment", length = 500)
    private String aiComment;

    @OneToMany(mappedBy = "outfit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    private List<OutfitItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "outfit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutfitAiTag> tags = new ArrayList<>();

    public Outfit(User user, String name, String memo, OutfitSource source) {
        this.user = user;
        this.name = name;
        this.memo = memo;
        this.source = source;
    }
}
