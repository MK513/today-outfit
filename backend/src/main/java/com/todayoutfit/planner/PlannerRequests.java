package com.todayoutfit.planner;

import jakarta.validation.constraints.NotNull;

public final class PlannerRequests {

    private PlannerRequests() {
    }

    /** PUT /planner/schedules/{planDate} 본문 */
    public record Upsert(@NotNull(message = "배치할 코디를 선택해주세요.") Long outfitId) {
    }
}
