package com.todayoutfit.clothing;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 쿼리 파라미터 color=블랙 → ClothingColor.BLACK.
 * (JSON 본문은 ClothingColor의 @JsonCreator가 처리한다) 허용값이 아니면 400 INVALID_REQUEST.
 */
@Configuration
public class ClothingColorParamConverter implements Converter<String, ClothingColor>, WebMvcConfigurer {

    @Override
    public ClothingColor convert(String source) {
        return ClothingColor.fromLabel(source);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(this);
    }
}
