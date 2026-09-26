package com.todayoutfit.clothing;

import java.time.LocalDateTime;
import java.util.function.UnaryOperator;

/** 명세의 Clothing 스키마 */
public record ClothingResponse(
        Long id,
        String name,
        ClothingCategory category,
        ClothingColor color,
        SeasonType season,
        String imageUrl,
        String imageFileName,
        ClothingSource source,
        LocalDateTime createdAt) {

    /** @param urlOf 저장소 키 → 공개 URL (ImageService::publicUrl) */
    public static ClothingResponse from(Clothing c, UnaryOperator<String> urlOf) {
        return new ClothingResponse(c.getId(), c.getName(), c.getCategory(), c.getColor(), c.getSeason(),
                urlOf.apply(c.getImageKey()), c.getImageFileName(), c.getSource(), c.getCreatedAt());
    }
}
