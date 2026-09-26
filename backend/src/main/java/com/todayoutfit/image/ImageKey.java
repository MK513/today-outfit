package com.todayoutfit.image;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 저장소 안에서 사진을 가리키는 키: {userId}/{uuid}.{jpg|png}.
 * DB(clothes.image_url)에는 이 키만 저장하고, 클라이언트에 줄 URL은 {@link ImageStore#publicUrl}로 만든다.
 * 형식이 엄격해서 경로 조작(../ 등)이 들어올 수 없다.
 */
record ImageKey(Long userId, String fileName, ImageType type) {

    private static final Pattern FORMAT = Pattern.compile(
            "(\\d+)/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png))");

    static ImageKey newKey(Long userId, ImageType type) {
        return new ImageKey(userId, UUID.randomUUID() + "." + type.extension, type);
    }

    static Optional<ImageKey> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher m = FORMAT.matcher(value);
        if (!m.matches()) {
            return Optional.empty();
        }
        return ImageType.fromExtension(m.group(3))
                .map(type -> new ImageKey(Long.valueOf(m.group(1)), m.group(2), type));
    }

    /** 사용자별 키의 공통 앞부분 (탈퇴 시 일괄 삭제용) */
    static String userPrefix(Long userId) {
        return userId + "/";
    }

    String value() {
        return userId + "/" + fileName;
    }

    @Override
    public String toString() {
        return value();
    }
}
