package com.todayoutfit.planner;

import com.todayoutfit.outfit.Outfit;
import com.todayoutfit.outfit.OutfitSummaryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Function;

/** 명세의 Schedule. updated_at은 다른 코디로 교체한 시각(처음 배치 시 null). */
public record ScheduleResponse(Long id, LocalDate planDate, OutfitSummaryResponse outfit, LocalDateTime updatedAt) {

    /** @param toSummary 코디 → 요약 (OutfitService::toSummary) */
    public static ScheduleResponse from(Schedule s, Function<Outfit, OutfitSummaryResponse> toSummary) {
        return new ScheduleResponse(s.getId(), s.getPlanDate(), toSummary.apply(s.getOutfit()), s.getUpdatedAt());
    }
}
