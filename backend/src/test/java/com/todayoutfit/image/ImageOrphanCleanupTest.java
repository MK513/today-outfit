package com.todayoutfit.image;

import static org.assertj.core.api.Assertions.assertThat;

import com.todayoutfit.IntegrationTest;
import com.todayoutfit.clothing.Clothing;
import com.todayoutfit.clothing.ClothingCategory;
import com.todayoutfit.clothing.ClothingColor;
import com.todayoutfit.clothing.ClothingRepository;
import com.todayoutfit.clothing.ClothingSource;
import com.todayoutfit.clothing.SeasonType;
import com.todayoutfit.user.User;
import com.todayoutfit.user.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

/** 업로드 후 의류로 저장되지 않은 사진은 24시간이 지나면 정리된다. */
@IntegrationTest
class ImageOrphanCleanupTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    @Autowired
    ImageService imageService;

    @Autowired
    ImageStore imageStore;

    @Autowired
    ImageProperties properties;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ClothingRepository clothingRepository;

    private String upload(User user) {
        String url = imageService.upload(user.getId(), new MockMultipartFile("image", "a.png", "image/png", PNG)).imageUrl();
        return imageStore.keyOf(url).orElseThrow();
    }

    private void age(String key, Duration age) throws Exception {
        Path file = properties.local().dir().resolve(key);
        Files.setLastModifiedTime(file, FileTime.from(Instant.now().minus(age)));
    }

    @Test
    void purgesOnlyOldUnreferencedImages() throws Exception {
        User user = userRepository.save(new User("o-" + UUID.randomUUID() + "@example.com", "x", "o", false));
        String oldOrphan = upload(user);
        String oldReferenced = upload(user);
        String freshOrphan = upload(user);
        clothingRepository.save(new Clothing(user, "옷", ClothingCategory.TOP, ClothingColor.BLACK, SeasonType.ALL,
                oldReferenced, "a.png", ClothingSource.MANUAL));
        age(oldOrphan, Duration.ofHours(25));
        age(oldReferenced, Duration.ofHours(25));

        imageService.purgeOrphansOlderThan(ImageService.ORPHAN_TTL);

        assertThat(imageStore.exists(oldOrphan)).isFalse();
        assertThat(imageStore.exists(oldReferenced)).isTrue();
        assertThat(imageStore.exists(freshOrphan)).isTrue();
    }
}
