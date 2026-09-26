package com.todayoutfit.image;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalImageStoreTest extends ImageStoreContractTest {

    @TempDir
    Path dir;

    @Override
    protected ImageStore createStore() {
        return new LocalImageStore(new ImageProperties("local", new ImageProperties.Local(dir, "/api/images")));
    }

    @Test
    void rejectsKeysEscapingStorageRoot() {
        assertThatThrownBy(() -> store.exists("../outside.png")).isInstanceOf(IllegalArgumentException.class);
    }
}
