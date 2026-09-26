package com.todayoutfit.clothing;

import org.springframework.data.jpa.domain.Specification;

/** 옷장 목록 필터. 값이 null인 조건은 적용하지 않는다. */
final class ClothingSpecs {

    private ClothingSpecs() {
    }

    static Specification<Clothing> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    static Specification<Clothing> categoryIs(ClothingCategory category) {
        return category == null ? null : (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    static Specification<Clothing> seasonIs(SeasonType season) {
        return season == null ? null : (root, query, cb) -> cb.equal(root.get("season"), season);
    }

    static Specification<Clothing> colorIs(ClothingColor color) {
        return color == null ? null : (root, query, cb) -> cb.equal(root.get("color"), color);
    }
}
