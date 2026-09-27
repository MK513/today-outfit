package com.todayoutfit.outfit;

import com.todayoutfit.clothing.ClothingSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.UnaryOperator;

/** 명세의 OutfitSummary: 코디 카드용 요약. thumbnails는 구성 순서 앞 3벌. */
public record OutfitSummaryResponse(
        Long id,
        String name,
        OutfitSource source,
        Integer aiScore,
        List<ClothingSummary> thumbnails,
        LocalDateTime createdAt) {

    static final int MAX_THUMBNAILS = 3;

    /** @param urlOf 저장소 키 → 공개 URL (ImageService::publicUrl) */
    public static OutfitSummaryResponse from(Outfit o, UnaryOperator<String> urlOf) {
        return new OutfitSummaryResponse(o.getId(), o.getName(), o.getSource(), o.getAiScore(),
                o.getItems().stream()
                        .limit(MAX_THUMBNAILS)
                        .map(item -> ClothingSummary.from(item.getClothing(), urlOf))
                        .toList(),
                o.getCreatedAt());
    }
}
