package com.todayoutfit.outfit;

import com.todayoutfit.clothing.ClothingSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.UnaryOperator;

/** 명세의 OutfitDetail. items는 item_order 오름차순. */
public record OutfitDetailResponse(
        Long id,
        String name,
        String memo,
        OutfitSource source,
        String requestText,
        String aiReason,
        Integer aiScore,
        String aiComment,
        List<String> aiTags,
        List<Item> items,
        LocalDateTime createdAt) {

    public record Item(int itemOrder, ClothingSummary clothing) {
    }

    /** @param urlOf 저장소 키 → 공개 URL (ImageService::publicUrl) */
    public static OutfitDetailResponse from(Outfit o, UnaryOperator<String> urlOf) {
        return new OutfitDetailResponse(o.getId(), o.getName(), o.getMemo(), o.getSource(), o.getRequestText(),
                o.getAiReason(), o.getAiScore(), o.getAiComment(),
                o.getTags().stream().map(OutfitAiTag::getTag).toList(),
                o.getItems().stream()
                        .map(i -> new Item(i.getItemOrder(), ClothingSummary.from(i.getClothing(), urlOf)))
                        .toList(),
                o.getCreatedAt());
    }
}
