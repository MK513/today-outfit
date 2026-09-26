package com.todayoutfit.clothing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ClothingRequests {

    private ClothingRequests() {
    }

    /** 명세의 ClothingCreateRequest */
    public record Create(
            @NotBlank(message = "의류 이름을 입력해주세요.")
            @Size(max = 50, message = "의류 이름은 50자 이하로 입력해주세요.")
            String name,

            @NotNull(message = "카테고리를 선택해주세요.")
            ClothingCategory category,

            @NotNull(message = "색상을 선택해주세요.")
            ClothingColor color,

            @NotNull(message = "계절을 선택해주세요.")
            SeasonType season,

            @NotNull(message = "등록 경로가 필요합니다.")
            ClothingSource source,

            @Size(max = 500)
            String imageUrl,

            @Size(max = 255)
            String imageFileName) {

        public Create {
            name = name == null ? null : name.trim();
            imageUrl = imageUrl == null || imageUrl.isBlank() ? null : imageUrl;
        }
    }

    /** 문장 등록 일괄 저장 (최대 8벌) */
    public record Bulk(
            @NotEmpty(message = "등록할 의류가 없습니다.")
            @Size(max = 8, message = "한 번에 8벌까지 등록할 수 있어요.")
            List<@Valid @NotNull Create> items) {
    }
}
