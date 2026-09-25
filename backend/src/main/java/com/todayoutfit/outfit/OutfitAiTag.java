package com.todayoutfit.outfit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "outfit_ai_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutfitAiTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @Column(nullable = false, length = 30)
    private String tag;

    public OutfitAiTag(Outfit outfit, String tag) {
        this.outfit = outfit;
        this.tag = tag;
    }
}
