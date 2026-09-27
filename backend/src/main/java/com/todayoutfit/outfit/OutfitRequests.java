package com.todayoutfit.outfit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;

public final class OutfitRequests {

    private OutfitRequests() {
    }

    /** 명세의 OutfitCreateRequest. clothing_ids 배열 순서가 표시 순서(item_order)가 된다. */
    public record Create(
            @NotBlank(message = "코디 이름을 입력해주세요.")
            @Size(max = 50, message = "코디 이름은 50자 이하로 입력해주세요.")
            String name,

            @Size(max = 500, message = "메모는 500자 이하로 입력해주세요.")
            String memo,

            @NotNull(message = "생성 경로가 필요합니다.")
            OutfitSource source,

            @NotEmpty(message = "코디에 담을 옷을 골라주세요.")
            @Size(max = 20, message = "코디에는 옷을 20벌까지 담을 수 있어요.")
            List<@NotNull Long> clothingIds,

            @Size(max = 500, message = "상황은 500자 이하로 입력해주세요.")
            String requestText,

            String aiReason,

            @Min(value = 0, message = "AI 점수는 0~100 사이여야 합니다.")
            @Max(value = 100, message = "AI 점수는 0~100 사이여야 합니다.")
            Integer aiScore,

            @Size(max = 500, message = "AI 코멘트는 500자 이하여야 합니다.")
            String aiComment,

            @Size(max = 5, message = "AI 태그는 5개까지 저장할 수 있어요.")
            List<@NotBlank(message = "빈 태그는 저장할 수 없어요.")
                 @Size(max = 30, message = "태그는 30자 이하여야 합니다.") String> aiTags) {

        public Create {
            name = trim(name);
            memo = trim(memo);
            requestText = trim(requestText);
        }

        @JsonIgnore
        @AssertTrue(message = "같은 옷을 두 번 담을 수 없어요.")
        public boolean isClothingIdsDistinct() {
            return clothingIds == null || new HashSet<>(clothingIds).size() == clothingIds.size();
        }
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
