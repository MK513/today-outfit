package com.todayoutfit.image;

import com.todayoutfit.clothing.ClothingRepository;
import com.todayoutfit.common.ApiException;
import com.todayoutfit.common.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * 의류 사진 규칙: 업로드 검증, 저장 키 발급, 소유권 확인, 삭제 · 정리.
 * 실제 파일 보관은 {@link ImageStore}에 맡기므로 저장소를 바꿔도 이 클래스는 그대로다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(ImageProperties.class)
public class ImageService {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    /** 업로드 후 의류로 저장되지 않은 사진을 보관하는 시간 (명세: 24시간 후 정리) */
    static final Duration ORPHAN_TTL = Duration.ofHours(24);

    private final ImageStore store;
    private final ClothingRepository clothingRepository;

    /** 형식(파일 시그니처) · 용량을 검증하고 저장한다. 응답의 image_url을 의류 등록 시 그대로 보내면 된다. */
    public ImageUploadResponse upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE_BYTES) {
            throw new ApiException(ErrorCode.INVALID_FILE);
        }
        ImageType type = detectType(file).orElseThrow(() -> new ApiException(ErrorCode.INVALID_FILE));

        ImageKey key = ImageKey.newKey(userId, type);
        try (InputStream in = file.getInputStream()) {
            store.put(key.value(), in, file.getSize(), type.mediaType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read upload", e);
        }
        return new ImageUploadResponse(store.publicUrl(key.value()), originalName(file));
    }

    /**
     * 클라이언트가 보낸 image_url이 이 사용자가 올린 사진이면 DB에 저장할 키를 돌려준다.
     * 남의 사진, 외부 URL, 형식이 틀리거나 이미 지워진 사진이면 empty.
     */
    public Optional<String> findOwnedKey(Long userId, String imageUrl) {
        return store.keyOf(imageUrl)
                .flatMap(ImageKey::parse)
                .filter(key -> key.userId().equals(userId))
                .map(ImageKey::value)
                .filter(store::exists);
    }

    /** DB에 저장된 키 → 응답용 URL. 키가 없으면 null */
    public String publicUrl(String key) {
        return key == null ? null : store.publicUrl(key);
    }

    /**
     * 트랜잭션이 커밋된 뒤, 더 이상 어떤 의류도 이 키를 쓰지 않으면 파일을 지운다.
     * (DB 삭제가 롤백되면 파일은 남는다)
     */
    public void deleteIfUnusedAfterCommit(String key) {
        if (key == null) {
            return;
        }
        afterCommit(() -> {
            if (!clothingRepository.existsByImageKey(key)) {
                store.delete(key);
            }
        });
    }

    /** 탈퇴한 사용자의 사진 전체를 트랜잭션 커밋 후 지운다. */
    public void deleteAllOfUserAfterCommit(Long userId) {
        afterCommit(() -> store.deleteAll(ImageKey.userPrefix(userId)));
    }

    /** 업로드 후 24시간이 지나도록 어떤 의류에도 연결되지 않은 사진을 지운다. */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void purgeOrphans() {
        purgeOrphansOlderThan(ORPHAN_TTL);
    }

    int purgeOrphansOlderThan(Duration ttl) {
        Instant cutoff = Instant.now().minus(ttl);
        int deleted = 0;
        for (ImageStore.StoredImage image : store.list()) {
            boolean managed = ImageKey.parse(image.key()).isPresent();
            if (managed && image.lastModified().isBefore(cutoff) && !clothingRepository.existsByImageKey(image.key())) {
                store.delete(image.key());
                deleted++;
            }
        }
        if (deleted > 0) {
            log.info("Purged {} orphan images", deleted);
        }
        return deleted;
    }

    private static Optional<ImageType> detectType(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return ImageType.detect(in.readNBytes(ImageType.SIGNATURE_LENGTH));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /** 경로 구분자를 제거한 원본 파일명 (의류 상세 화면 표시용) */
    private static String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return null;
        }
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
