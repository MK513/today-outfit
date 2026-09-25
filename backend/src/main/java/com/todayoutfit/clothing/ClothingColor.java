package com.todayoutfit.clothing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;

/** 색상은 API·DB 모두 화면 표시값(한글) 그대로 주고받는다. */
@Getter
public enum ClothingColor {
    BLACK("블랙"),
    WHITE("화이트"),
    GRAY("그레이"),
    BEIGE("베이지"),
    BROWN("브라운"),
    BLUE("블루"),
    GOLD("골드");

    @JsonValue
    private final String label;

    ClothingColor(String label) {
        this.label = label;
    }

    @JsonCreator
    public static ClothingColor fromLabel(String label) {
        return Arrays.stream(values())
                .filter(c -> c.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown color: " + label));
    }
}
