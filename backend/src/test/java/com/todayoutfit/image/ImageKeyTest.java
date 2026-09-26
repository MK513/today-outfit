package com.todayoutfit.image;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImageKeyTest {

    @Test
    void newKeyRoundTrips() {
        ImageKey key = ImageKey.newKey(42L, ImageType.PNG);

        assertThat(key.value()).matches("42/[0-9a-f-]{36}\\.png");
        assertThat(ImageKey.parse(key.value())).contains(key);
    }

    @Test
    void parseRejectsAnythingButTheStrictFormat() {
        assertThat(ImageKey.parse(null)).isEmpty();
        assertThat(ImageKey.parse("42/../../etc/passwd")).isEmpty();
        assertThat(ImageKey.parse("42/11111111-1111-1111-1111-111111111111.gif")).isEmpty();
        assertThat(ImageKey.parse("/42/11111111-1111-1111-1111-111111111111.png")).isEmpty();
        assertThat(ImageKey.parse("abc/11111111-1111-1111-1111-111111111111.png")).isEmpty();
    }

    @Test
    void parseDetectsTypeFromExtension() {
        assertThat(ImageKey.parse("1/11111111-1111-1111-1111-111111111111.jpg"))
                .get().extracting(ImageKey::type).isEqualTo(ImageType.JPEG);
    }
}
