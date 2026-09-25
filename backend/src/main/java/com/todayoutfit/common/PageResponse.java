package com.todayoutfit.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** 목록 API 공통 응답(PagedResponse). page는 1부터 시작한다. */
public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int page, int size) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber() + 1,
                page.getSize());
    }
}
