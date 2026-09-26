package com.todayoutfit.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 모든 {@link ImageStore} 구현체가 지켜야 할 동작. 새 저장소(S3 등)를 추가하면 이 클래스를 상속해
 * {@link #createStore()}만 구현하고 같은 테스트를 통과시킨다.
 */
abstract class ImageStoreContractTest {

    static final byte[] BYTES = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    static final String KEY_A = "7/11111111-1111-1111-1111-111111111111.png";
    static final String KEY_B = "7/22222222-2222-2222-2222-222222222222.png";
    static final String KEY_OTHER_USER = "8/33333333-3333-3333-3333-333333333333.png";

    protected ImageStore store;

    /** 비어 있는 새 저장소 */
    protected abstract ImageStore createStore() throws Exception;

    @BeforeEach
    void setUpStore() throws Exception {
        store = createStore();
    }

    private void put(String key) {
        store.put(key, new ByteArrayInputStream(BYTES), BYTES.length, MediaType.IMAGE_PNG);
    }

    @Test
    void putThenGetReturnsSameBytes() throws Exception {
        put(KEY_A);

        assertThat(store.exists(KEY_A)).isTrue();
        try (InputStream in = store.get(KEY_A).orElseThrow().getInputStream()) {
            assertThat(in.readAllBytes()).isEqualTo(BYTES);
        }
    }

    @Test
    void missingKeyIsEmpty() {
        assertThat(store.exists(KEY_A)).isFalse();
        assertThat(store.get(KEY_A)).isEmpty();
    }

    @Test
    void deleteRemovesOnlyThatKeyAndIgnoresMissing() {
        put(KEY_A);
        put(KEY_B);

        store.delete(KEY_A);
        store.delete(KEY_A);

        assertThat(store.exists(KEY_A)).isFalse();
        assertThat(store.exists(KEY_B)).isTrue();
    }

    @Test
    void deleteAllRemovesOnlyThatPrefix() {
        put(KEY_A);
        put(KEY_B);
        put(KEY_OTHER_USER);

        store.deleteAll("7/");

        assertThat(store.exists(KEY_A)).isFalse();
        assertThat(store.exists(KEY_B)).isFalse();
        assertThat(store.exists(KEY_OTHER_USER)).isTrue();
    }

    @Test
    void listReturnsAllKeysWithModifiedTime() {
        Instant before = Instant.now().minusSeconds(5);
        put(KEY_A);
        put(KEY_OTHER_USER);

        assertThat(store.list())
                .extracting(ImageStore.StoredImage::key)
                .containsExactlyInAnyOrder(KEY_A, KEY_OTHER_USER);
        assertThat(store.list()).allSatisfy(image -> assertThat(image.lastModified()).isAfter(before));
    }

    @Test
    void publicUrlRoundTripsToKey() {
        String url = store.publicUrl(KEY_A);

        assertThat(url).isNotEqualTo(KEY_A);
        assertThat(store.keyOf(url)).contains(KEY_A);
        assertThat(store.keyOf("https://elsewhere.example.com/" + KEY_A)).isEmpty();
        assertThat(store.keyOf(null)).isEmpty();
    }
}
