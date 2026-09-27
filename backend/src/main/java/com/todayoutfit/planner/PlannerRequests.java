package com.todayoutfit.planner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class PlannerRequests {

    private PlannerRequests() {
    }

    /** PUT /planner/schedules/{planDate} 본문 */
    public record Upsert(@NotNull(message = "배치할 코디를 선택해주세요.") Long outfitId) {
    }

    /** POST /planner/weekly-outfits 본문: AI 주간 추천 결과(최대 7일)를 한 번에 저장 */
    public record Weekly(
            @NotBlank(message = "상황을 입력해주세요.")
            @Size(max = 500, message = "상황은 500자 이하로 입력해주세요.")
            String requestText,

            @NotEmpty(message = "저장할 날짜가 없습니다.")
            @Size(max = 7, message = "주간 코디는 7일까지 저장할 수 있어요.")
            List<@Valid @NotNull Day> days) {

        public Weekly {
            requestText = requestText == null ? null : requestText.trim();
        }

        @JsonIgnore
        @AssertTrue(message = "같은 날짜를 두 번 저장할 수 없어요.")
        public boolean isPlanDatesDistinct() {
            if (days == null) {
                return true;
            }
            List<LocalDate> dates = days.stream().filter(Objects::nonNull).map(Day::planDate).toList();
            return new HashSet<>(dates).size() == dates.size();
        }
    }

    public record Day(
            @NotNull(message = "날짜가 필요합니다.")
            LocalDate planDate,

            @NotEmpty(message = "코디에 담을 옷이 없습니다.")
            @Size(max = 20, message = "코디에는 옷을 20벌까지 담을 수 있어요.")
            List<@NotNull Long> clothingIds,

            @NotBlank(message = "추천 이유가 필요합니다.")
            String aiReason) {

        @JsonIgnore
        @AssertTrue(message = "같은 옷을 두 번 담을 수 없어요.")
        public boolean isClothingIdsDistinct() {
            return clothingIds == null || new HashSet<>(clothingIds).size() == clothingIds.size();
        }
    }
}
