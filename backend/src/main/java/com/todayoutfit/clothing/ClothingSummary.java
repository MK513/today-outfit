package com.todayoutfit.clothing;

import java.util.function.UnaryOperator;

/** 명세의 ClothingSummary: 코디 · 플래너 · 챌린지 화면의 썸네일용 의류 요약 */
public record ClothingSummary(Long id, String name, ClothingCategory category, ClothingColor color, String imageUrl) {

    /** @param urlOf 저장소 키 → 공개 URL (ImageService::publicUrl) */
    public static ClothingSummary from(Clothing c, UnaryOperator<String> urlOf) {
        return new ClothingSummary(c.getId(), c.getName(), c.getCategory(), c.getColor(), urlOf.apply(c.getImageKey()));
    }
}
