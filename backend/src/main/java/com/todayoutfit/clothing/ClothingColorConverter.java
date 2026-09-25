package com.todayoutfit.clothing;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClothingColorConverter implements AttributeConverter<ClothingColor, String> {

    @Override
    public String convertToDatabaseColumn(ClothingColor color) {
        return color == null ? null : color.getLabel();
    }

    @Override
    public ClothingColor convertToEntityAttribute(String label) {
        return label == null ? null : ClothingColor.fromLabel(label);
    }
}
