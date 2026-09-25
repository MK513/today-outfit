package com.todayoutfit.clothing;

import com.todayoutfit.common.BaseTimeEntity;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "clothes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clothing extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClothingCategory category;

    @Column(nullable = false, length = 10)
    private ClothingColor color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeasonType season;

    /** NULL이면 화면에서 카테고리+색상 대표 사진으로 대체한다. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_file_name")
    private String imageFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClothingSource source;

    public Clothing(User user, String name, ClothingCategory category, ClothingColor color, SeasonType season,
            String imageUrl, String imageFileName, ClothingSource source) {
        this.user = user;
        this.name = name;
        this.category = category;
        this.color = color;
        this.season = season == null ? SeasonType.ALL : season;
        this.imageUrl = imageUrl;
        this.imageFileName = imageFileName;
        this.source = source;
    }
}
