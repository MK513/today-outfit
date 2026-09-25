package com.todayoutfit.outfit;

import com.todayoutfit.clothing.Clothing;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 코디 ↔ 의류 연결. 의류가 삭제되면 이 행만 DB에서 CASCADE 삭제되고 코디는 남는다. */
@Getter
@Entity
@Table(name = "outfit_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutfitItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothing_id", nullable = false)
    private Clothing clothing;

    /** 표시 순서 (1부터) */
    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    public OutfitItem(Outfit outfit, Clothing clothing, int itemOrder) {
        this.outfit = outfit;
        this.clothing = clothing;
        this.itemOrder = itemOrder;
    }
}
